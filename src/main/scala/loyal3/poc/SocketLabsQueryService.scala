package loyal3.poc

import java.text.SimpleDateFormat
import java.util.Calendar
import scala.collection.mutable.{ HashMap, ListBuffer, Map }
import scala.xml.NodeSeq
import scala.xml.XML
import org.joda.time.DateTime
import com.loyal3.model.email.MongoEmail
import com.loyal3.model.email.SocketLabsApiCall
import com.loyal3.util.logging.Logging
import dispatch.Request.toHandlerVerbs
import dispatch.Request.toRequestVerbs
import dispatch.Http
import dispatch.StatusCode
import dispatch.url
import loyal3.poc.utils.HibernateUtil
import com.loyal3.util.IdFactory
import com.loyal3.util.TimeSource
import com.loyal3.model.email.MongoEmailDAO
import com.mongodb.WriteConcern
import org.hibernate.criterion.{ Property, Restrictions }
import org.hibernate.criterion.ProjectionList
import org.hibernate.criterion.Projections
import org.hibernate.Criteria
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.HibernateException
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.MongoException
import org.hibernate.classic.Session

/**
 * An asynchronous query service for returning Bounced and Feedback Loop emails 
 * from SocketLabs. 
 */
class SocketLabsQueryService extends Object with Logging {

  def runMessagesFailed(): Unit = {
    val socketLabsApiCall: SocketLabsApiCall = getMessagesData("messagesFailed")
    processApiResponse(socketLabsApiCall)
  }

  def runMessagesFblReported(): Unit = {
    val socketLabsApiCall: SocketLabsApiCall = getMessagesData("messagesFblReported")
    processApiResponse(socketLabsApiCall)
  }

  private def getMessagesData(methodName: String) = {
    val lastCallSocketLabsApiCall:SocketLabsApiCall	=	lastCallOf(methodName)
    val windowParams: Map[String, String]	=	calculateWindowArgs(lastCallSocketLabsApiCall)
    val socketLabsApiCall:SocketLabsApiCall	=	createApiCall(methodName,windowParams)
    performApiCall(methodName,windowParams,socketLabsApiCall)
    socketLabsApiCall
  }

  /**
   * 
   */
  def performApiCall(method: String, windowParams: Map[String, String], socketLabsApiCall: SocketLabsApiCall): SocketLabsApiCall = {
    var xml: String = ""
    //var socketLabsApiCall = new SocketLabsApiCall
    val http = new Http
    try {
      val request = constructRequest(method, windowParams)
      debug("request url: " + request.toString())
      println("request url: " + request.to_uri.toURL().toString())
      //val res: Promise[Either[Throwable, xml.Elem]]	=	Http(request).either
      xml = http(request >~ { _.getLines.mkString })
      //println("xml:"+xml)
      socketLabsApiCall.setRawResponse(xml)
      socketLabsApiCall.setHttpStatus("200")
    } catch {
      case sc: StatusCode =>
        socketLabsApiCall.setHttpStatus(sc.code.toString())
        debug("perform_api_call returned status code : " + sc.code.toString())
      case ex: Exception =>
        error("Failed to perform_api_call:" + ex.getMessage())
        http.shutdown()
        ex.printStackTrace()
    }
    http.shutdown()
    //debug("In perform_api_call XML response: " + xml)
    socketLabsApiCall
  }

  def constructRequest(method: String, windowParams: Map[String, String]) =
    make_auth_url_method(method).as(SocketLabsQueryService.SOCKET_LABS_API_USER, SocketLabsQueryService.SOCKET_LABS_API_KEY) <<? addQueryParams(windowParams)

  private def make_auth_url_method(method: String) =
    url("https://" + SocketLabsQueryService.SOCKET_LABS_API_HOST + "/" + SocketLabsQueryService.SOCKET_LABS_API_PREFIX + "/" + method)

  private def addQueryParams(queryParams: Map[String, String]) = {
    queryParams += ("accountId" -> SocketLabsQueryService.SOCKET_LABS_ACCOUNT_ID)
    queryParams += ("type" -> "xml")
  }

  /**
   * Create and persist initial Hibernate data record prior to issuing a request to 
   * SocketLabs. 
   * 
   * @param methodName SocketLabs URI
   * @param windowParams Date range parameters
   * @return SocketLabsApiCall container
   */
def createApiCall(methodName: String, windowParams: Map[String, String]) = {
    val socketLabsApiCall = new SocketLabsApiCall
    socketLabsApiCall.setId(com.loyal3.util.IdFactory.generateId)
    socketLabsApiCall.setStartDate(java.sql.Date.valueOf(windowParams.get("startDate").get))
    socketLabsApiCall.setEndDate(java.sql.Date.valueOf(windowParams.get("endDate").get))
    socketLabsApiCall.setIndexVal(Some(windowParams.get("index").get.toLong))
    socketLabsApiCall.setMethodName(methodName)
    socketLabsApiCall.createdAt = new DateTime()
    var session:Session = null  
    try {
      session = HibernateUtil.factory.openSession();
      val tx = session.beginTransaction();
      //session.setFlushMode(FlushMode.AUTO)
      session.save(socketLabsApiCall);
      session.flush();
      tx.commit()
      session.close();
    } catch {
      case he: HibernateException =>
        if(null!=session)
        	session.close();
        error("Hibernate Exception : " + he.getMessage())
        he.printStackTrace()
      case ex: Exception =>
        error("Failed to create_api_call : " + ex.getMessage())
        ex.printStackTrace() 
    }
    //println("create id = " + socketLabsApiCall.getId())
    socketLabsApiCall
  }

  /**
   * Update and persist Hibernate data record with end time, after 
   * successful processing of SocketLabs records. 
   * 
   * @param socketLabsApiCall
   * @return SocketLabsApiCall container
   */
def updateAttributes(socketLabsApiCall: SocketLabsApiCall) = {
  socketLabsApiCall.updatedAt = new DateTime()      
  var session:Session = null
  try {
      //println("update id = " + socketLabsApiCall.getId())
      session = HibernateUtil.factory.openSession();
      val tx = session.beginTransaction();
      //session.setFlushMode(FlushMode.AUTO)
      session.saveOrUpdate(socketLabsApiCall);
      session.flush();
      tx.commit()
      session.close();
    } catch {
      case he: HibernateException =>
        if(null!=session)
          session.close()
        error("Hibernate failed during updateAttributes : " + he.getMessage())
        he.printStackTrace();
      case ex: Exception =>
        error("Failed to updateAttributes : " + ex.getMessage())
        ex.printStackTrace()
    }
  }

  def processApiResponse(socketLabsApiCall: SocketLabsApiCall) = {
    debug("raw response : " + socketLabsApiCall.getRawResponse())
    //println("raw response : "+socketLabsApiCall.getRawResponse())
    var mongoEmailList: Array[MongoEmail] = null
    if (socketLabsApiCall.getHttpStatus() == null)
      socketLabsApiCall.setCount(Some(0))
    updateAttributes(socketLabsApiCall);
    if (socketLabsApiCall.getRawResponse() != null) {
      val responseXml: scala.xml.Elem = XML.loadString(socketLabsApiCall.getRawResponse())
      val api_count: Int = extractApiCount(responseXml)
      println("api_count = " + api_count)
      if (api_count > 0) {
        val seq: Seq[scala.xml.NodeSeq] = extractItemsArray(responseXml)
        val validItemsList: ListBuffer[scala.xml.NodeSeq] = extractValidItems(seq)
        if (socketLabsApiCall.getMethodName() == "messagesFailed")
          mongoEmailList = recordDeliveryFailure(socketLabsApiCall, validItemsList, api_count)
        else if (socketLabsApiCall.getMethodName() == "messagesFblReported")
          mongoEmailList = recordFeedback(socketLabsApiCall, validItemsList, api_count)
      }

    }
    mongoEmailList
  }

  /**
   * Persists feedback loop email to MongoDB.
   *
   * @param socketLabsApiCall
   * @param validItemsList
   * @param api_count
   * @return
   *
   */
  def recordFeedback(socketLabsApiCall: SocketLabsApiCall, validItemsList: ListBuffer[scala.xml.NodeSeq], api_count: Int) = {
    val mongoEmailList = new Array[MongoEmail](validItemsList.size)
    var i = 0
    var hasError = false

    validItemsList.iterator foreach (item => {
      val userId = IdFactory.generateId()
      val toAddr = (item \\ "ToAddress").text
      val fromAddr = (item \\ "FromAddress").text
      val date = TimeSource.stringToTimeSource((item \\ "DateTime").text).toDate()
      val failureType = (item \\ "FailureType").text
      val messageId = (item \\ "MessageId").text

      val email = MongoEmail(
          userId, 
          toAddr, 
          fromAddr, 
          "NULL", //Subject 
          "<html/>", 
          date, 
          null, 
          null, 
          null, 
          null, 
          null, 
          messageId)
          
      try {
        val mongoId = MongoEmailDAO.insert(email, WriteConcern.NORMAL)
        mongoEmailList(i) = email
        i += 1;
      } catch {
        case me: MongoException => 
          hasError = true
          me.printStackTrace()
        case ex: Exception =>
          error("Failed to record feedback " + ex.getMessage())
          hasError = true
          ex.printStackTrace()
      }
    })
    if (!hasError) {
      socketLabsApiCall.setCount(Some(api_count))
      updateAttributes(socketLabsApiCall);
    }

    mongoEmailList
  }
  /**
   * Persists email delivery failures to MongoDB.
   *
   * @param socketLabsApiCall
   * @param validItemsList
   * @param api_count
   * @return
   *
   */
  def recordDeliveryFailure(socketLabsApiCall: SocketLabsApiCall, validItemsList: ListBuffer[scala.xml.NodeSeq], api_count: Int) = {
    val mongoEmailList = new Array[MongoEmail](validItemsList.size)
    var i: Int = 0
    var hasError = false

    validItemsList.iterator foreach (item => {
      val userId = IdFactory.generateId()
      val toAddr = (item \\ "ToAddress").text
      val fromAddr = (item \\ "FromAddress").text
      val date = TimeSource.stringToTimeSource((item \\ "DateTime").text).toDate()
      val failureType = (item \\ "FailureType").text
      val messageId = (item \\ "MessageId").text
      
      val email = MongoEmail(
          userId, 
          toAddr, 
          fromAddr, 
          "NULL", //Subject
          "<html/>", 
          date, 
          null, 
          null, 
          null, 
          null, 
          null, 
          messageId);
      
      try {
        val mongoId = MongoEmailDAO.insert(email, WriteConcern.NORMAL)
        mongoEmailList(i) = email
        i += 1;
      } catch {
        case me: MongoException =>
          hasError = true
          me.printStackTrace()
      	case ex: Exception =>
          error("Failed to record DeliveryFailure " + ex.getMessage())
          hasError = true
          ex.printStackTrace()
      }
    })

    if (!hasError) {
      socketLabsApiCall.setCount(Some(api_count))
      updateAttributes(socketLabsApiCall);
    }

    mongoEmailList
  }

  def extractApiCount(xml: scala.xml.Elem) = {
    val seq = for {
      resp_elem <- xml \\ "response"
      if (!resp_elem.isEmpty)
      count_elem <- xml \\ "count"
    } yield count_elem.text.toInt
    seq.head
  }

  private def extractItemsArray(xml: scala.xml.Elem) = {
    val seq = for {
      resp_elem <- xml \\ "response"
      if (!resp_elem.isEmpty)
      collection_elem <- xml \\ "collection"
      if (!collection_elem.isEmpty)
      item_elem <- xml \\ "item"
    } yield item_elem
    seq
  }

  def extractValidItems(itemSeq: Seq[scala.xml.NodeSeq]) = {
    var validItems = new ListBuffer[NodeSeq]
    for (item <- itemSeq) {
      val message_id = item \\ "MessageId"
      if (!message_id.isEmpty && message_id.text.startsWith(message_namespace))
        validItems += item
    }
    debug("validItems =" + validItems)
    //println("validItems ="+validItems)
    validItems
  }

  def message_namespace = "de"

  /**
   * If available, query for and return the SocketLabsApiCall object with the most recent
   * creation date.
   *
   * Creates a sub-query for MAX(created_at) and retrieves a single result, or null, from
   * table `socket_labs_api_calls` with a matching creation date.
   *
   * @param method_name
   * @return
   */
  def lastCallOf(method_name: String) = {
    var socketLabsApiCall: SocketLabsApiCall = null
    var session:Session = null
    try {
      session = HibernateUtil.factory.openSession();

      val lastCallDate = DetachedCriteria.forClass(classOf[SocketLabsApiCall])
        .setProjection(Projections.max("createdAt"))

      socketLabsApiCall = session.createCriteria(classOf[SocketLabsApiCall])
        .add(Restrictions.like("methodName", method_name))
        .add(Property.forName("createdAt").eq(lastCallDate))
        .uniqueResult()
        .asInstanceOf[SocketLabsApiCall]

      session.flush()
      session.close()
    } catch {
      case he: HibernateException =>
        if(session!=null)
        	session.close()
        he.printStackTrace()
      case ex: Exception =>
        error("Failed to execute lastCallOf : " + ex.getMessage())
        ex.printStackTrace()
    }
    
    socketLabsApiCall
  }

  /**
   * Calculate date range
   * @param socketLabsApiCall
   * @return
   */
def calculateWindowArgs(socketLabsApiCall:SocketLabsApiCall):Map[String, String] ={
    var start_date=""
    var end_date=""
    var index	=""
    var windowArgsMap:Map[String,String]	=	new HashMap[String, String]
    try{
      println("socketLabsApiCall : "+socketLabsApiCall)
      if(socketLabsApiCall==null)
        windowArgsMap+=("startDate"->SocketLabsQueryService.defaultDate(),
          "endDate"->SocketLabsQueryService.today(),
          "index"->"0")
      else if(socketLabsApiCall.getHttpStatus()==null || socketLabsApiCall.getCount()==None){//previous call not fully processed
        println("in else if socketLabsApiCall.getHttpStatus()==null....")
        windowArgsMap+=("startDate"->socketLabsApiCall.getStartDate().toString(),
          "endDate"->SocketLabsQueryService.today().toString(),
          "index"->socketLabsApiCall.getIndexVal().get.toString())
      }else{
           start_date	=	socketLabsApiCall.getStartDate().toString()

        if((!socketLabsApiCall.getStartDate().toString().isEmpty()) &&
          (socketLabsApiCall.getEndDate().getTime()-socketLabsApiCall.getStartDate().getTime())>0 &&
          socketLabsApiCall.getCount().get<SocketLabsQueryService.max_items_returned_per_call){
          start_date	=	socketLabsApiCall.getEndDate().toString()
          index	=	"0";
        }else
          index	=	(socketLabsApiCall.getIndexVal().get + socketLabsApiCall.getCount().get).toString()
        windowArgsMap+=("startDate"->start_date,
          "endDate"->SocketLabsQueryService.today().toString(),
          "index"->index)
      }
    }catch{
      case ex:Exception => error("Failed to calculate_window_params: "+ex.getMessage())
      ex.printStackTrace()
    }
    println("windowArgsMap = "+windowArgsMap)
    windowArgsMap
  }
}

object SocketLabsQueryService {
  val SOCKET_LABS_API_USER = "loyal3"
  val SOCKET_LABS_API_KEY = "d8f7d5b39ccb2d65b974"
  val SOCKET_LABS_API_HOST = "api.socketlabs.com"
  val SOCKET_LABS_API_PREFIX = "v1"
  val SOCKET_LABS_ACCOUNT_ID = "1143"
  val max_items_returned_per_call = 500

  def defaultDate(): String = {
    val formatString = "yyyy-MM-dd"
    val cal = Calendar.getInstance
    cal.add(Calendar.DATE, -14) //today - 2.weeks
    println("default date time : " + cal.getTime())
    new SimpleDateFormat(formatString) format cal.getTime
  }

  def today(): String = {
    val formatString = "yyyy-MM-dd"
    val cal = Calendar.getInstance
    new SimpleDateFormat(formatString) format cal.getTime
  }


  def main(args: Array[String]): Unit = {
    val queryParams = Map("startDate" -> "2012-11-18",
      "endDate" -> "2012-11-30",
      "index" -> "0")
    val restClient = new SocketLabsQueryService
    restClient.runMessagesFailed()
    restClient.runMessagesFblReported();
    //println(MongoEmailDAO.findOne(MongoDBObject("html_body" -> "<html/>")));
  }
}
