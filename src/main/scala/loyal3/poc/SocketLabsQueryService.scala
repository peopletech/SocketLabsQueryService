package loyal3.poc

import java.text.SimpleDateFormat
import java.util.Calendar
import scala.collection.mutable.Map
import com.loyal3.model.email.SocketLabsApiCall
import dispatch.Request.toHandlerVerbs
import dispatch.Request.toRequestVerbs
import scala.xml.XML
import dispatch.url
import dispatch.StatusCode
import loyal3.poc.utils.HibernateUtil
import dispatch.Http
import com.loyal3.util.logging.Logging
import scala.collection.mutable.HashMap
import scala.xml.NodeSeq
import scala.collection.mutable.ListBuffer
import com.loyal3.model.email.BouncedEmail
import org.joda.time.DateTime
import _root_.com.loyal3.model.email._
import org.hibernate.FlushMode
//import com.loyal3.model.email.MongoEmailDAO






class SocketLabsQueryService extends Object with Logging{

  def runMessagesFailed():Unit={
    val socketLabsApiCall:SocketLabsApiCall	=	getMessagesData("messagesFailed")
    processApiResponse(socketLabsApiCall)
  }

  def runMessagesFblReported():Unit={
    val socketLabsApiCall:SocketLabsApiCall	=	getMessagesData("messagesFblReported")
    processApiResponse(socketLabsApiCall)
  }

  private def getMessagesData(methodName:String)={
    val lastCallSocketLabsApiCall:SocketLabsApiCall	=	lastCallOf(methodName)
    val windowParams: Map[String, String]	=	calculateWindowArgs(lastCallSocketLabsApiCall)
    val socketLabsApiCall:SocketLabsApiCall	=	createApiCall(methodName,windowParams)
    performApiCall(methodName,windowParams,socketLabsApiCall)
    socketLabsApiCall
  }

  private def performApiCall(method: String, windowParams: Map[String, String], socketLabsApiCall:SocketLabsApiCall) :SocketLabsApiCall ={
    var xml:String	= ""
    //var socketLabsApiCall = new SocketLabsApiCall
    val http = new Http
    try{
      val request	=	constructRequest(method, windowParams)
      debug("request url: "+request.toString())
      println("request url: "+request.to_uri.toURL().toString())
      //val res: Promise[Either[Throwable, xml.Elem]]	=	Http(request).either
      xml	=	http(request >~ { _.getLines.mkString })
      //println("xml:"+xml)
      socketLabsApiCall.setRawResponse(xml)
      socketLabsApiCall.setHttpStatus("200")
    }catch{
      case sc:StatusCode => socketLabsApiCall.setHttpStatus(sc.code.toString())
      debug("perform_api_call returned status code : "+sc.code.toString())
      case ex:Exception => error("Failed to perform_api_call:"+ex.getMessage())
      ex.printStackTrace()
    }
    http.shutdown()
    debug("In perform_api_call XML response: "+xml)
    socketLabsApiCall
  }

  private def constructRequest(method:String, windowParams: Map[String, String])=
    make_auth_url_method(method).as(SocketLabsQueryService.SOCKET_LABS_API_USER,SocketLabsQueryService.SOCKET_LABS_API_KEY) <<? addQueryParams(windowParams)

  private def make_auth_url_method(method: String)=
    url("https://"+SocketLabsQueryService.SOCKET_LABS_API_HOST+"/"+SocketLabsQueryService.SOCKET_LABS_API_PREFIX+"/"+method)


  private def addQueryParams(queryParams : Map[String, String])={
    queryParams += ("accountId" -> SocketLabsQueryService.SOCKET_LABS_ACCOUNT_ID)
    queryParams += ("type" -> "xml")
  }



  // Creates database record for this call
  private def createApiCall(methodName:String, windowParams: Map[String, String])={
    val socketLabsApiCall = new SocketLabsApiCall
    try{
      socketLabsApiCall.setId(com.loyal3.util.IdFactory.generateId)
      socketLabsApiCall.setStartDate(java.sql.Date.valueOf(windowParams.get("startDate").get))
      socketLabsApiCall.setEndDate(java.sql.Date.valueOf(windowParams.get("endDate").get))
      socketLabsApiCall.setIndexVal(Some(windowParams.get("index").get.toLong))
      socketLabsApiCall.setMethodName(methodName)
       val session = HibernateUtil.factory.openSession();
        val tx	=	session.beginTransaction();
        //session.setFlushMode(FlushMode.AUTO)
        session.save(socketLabsApiCall);
        session.flush();
        tx.commit()
        session.close();
    }catch{
      case ex:Exception => error("Failed to create_api_call : "+ex.getMessage())
      ex.printStackTrace()
    }
    println("create id = "+socketLabsApiCall.getId())
    socketLabsApiCall
  }

  private def updateAttributes(socketLabsApiCall: SocketLabsApiCall)={
    try{
      println("update id = "+socketLabsApiCall.getId())
    val session = HibernateUtil.factory.openSession();
    val tx	=	session.beginTransaction();
    //session.setFlushMode(FlushMode.AUTO)
    session.saveOrUpdate(socketLabsApiCall);
    //session.merge(socketLabsApiCall);
    session.flush();
    tx.commit()
    session.close();
    }catch{
      case ex:Exception => error("Failed to updateAttributes : "+ex.getMessage())
      ex.printStackTrace()
    }
  }


  private def processApiResponse(socketLabsApiCall:SocketLabsApiCall){
    debug("raw response : "+socketLabsApiCall.getRawResponse())
    //println("raw response : "+socketLabsApiCall.getRawResponse())
    if(socketLabsApiCall.getHttpStatus()==null)
      socketLabsApiCall.setCount(Some(0))
    updateAttributes(socketLabsApiCall);
    if(!socketLabsApiCall.getRawResponse().isEmpty()){
	    val responseXml: scala.xml.Elem	=	XML.loadString(socketLabsApiCall.getRawResponse())
	    val api_count: Int	=	extractApiCount(responseXml)
	    println("api_count = "+api_count)
	    if (api_count>0)  {
	      val seq: Seq[scala.xml.NodeSeq]	=	extractItemsArray(responseXml)
	      //println(seq)
	      val validItemsList:ListBuffer[scala.xml.NodeSeq]=extractValidItems(seq)
	      if (socketLabsApiCall.getMethodName()=="messagesFailed")
	        recordDeliveryFailure(socketLabsApiCall, validItemsList, api_count)
	      else if (socketLabsApiCall.getMethodName()=="messagesFblReported")
	        recordFeedback(socketLabsApiCall, validItemsList, api_count)
	    }
	    
  	}
  }

  def recordFeedback(socketLabsApiCall:SocketLabsApiCall, validItemsList:ListBuffer[scala.xml.NodeSeq], api_count: Int)={
    validItemsList.iterator foreach(item=>{
      val bouncedEmail =	new BouncedEmail
      val fromAddr	=	item \\ "FromAddress"
      bouncedEmail.setFromAddress(fromAddr.text)
      val toAddr	=	item \\ "ToAddress"
      bouncedEmail.setToAddress(toAddr.text)
      val dateTime	=	item \\ "DateTime"
      bouncedEmail.setBouncedAt(new DateTime(dateTime.text))
      val failureType	=	item \\ "FailureType"
      bouncedEmail.setFailureType(failureType.text)
      val messageId	=	item \\ "MessageId"
      bouncedEmail.setId(messageId.text)
      
      val mongoEmailDao = MongoEmailDAO.findOneById("String")
      
      //new com.loyal3.model.email.MongoEmailDAO$.MongoEmailDAO
      socketLabsApiCall.setCount(Some(api_count))
      updateAttributes(socketLabsApiCall);
    })

  }

  def recordDeliveryFailure(socketLabsApiCall:SocketLabsApiCall, validItemsList:ListBuffer[scala.xml.NodeSeq], api_count: Int)={
    validItemsList.iterator foreach(item=>{
      val bouncedEmail =	new BouncedEmail
      val fromAddr	=	item \\ "FromAddress"
      bouncedEmail.setFromAddress(fromAddr.text)
      val toAddr	=	item \\ "ToAddress"
      bouncedEmail.setToAddress(toAddr.text)
      val dateTime	=	item \\ "DateTime"
      bouncedEmail.setBouncedAt(new DateTime(dateTime.text))
      val failureType	=	item \\ "FailureType"
      bouncedEmail.setFailureType(failureType.text)
      val messageId	=	item \\ "MessageId"
      bouncedEmail.setId(messageId.text)
      //val mongoEmailDao =	new MongoEmailDAO
      socketLabsApiCall.setCount(Some(api_count))
      updateAttributes(socketLabsApiCall);
    })

  }

  private def extractApiCount(xml: scala.xml.Elem) = {
    val seq = for{
      resp_elem <- xml \\ "response"
      if(!resp_elem.isEmpty)
      count_elem <- xml \\ "count"
    } yield count_elem.text.toInt
    seq.head
  }


  private def extractItemsArray(xml: scala.xml.Elem) = {
    val seq = for{
      resp_elem <- xml \\ "response"
      if(!resp_elem.isEmpty)
      collection_elem <- xml \\ "collection"
      if(!collection_elem.isEmpty)
      item_elem <- xml \\ "item"
    } yield item_elem
    seq
  }


  private def extractValidItems(itemSeq: Seq[scala.xml.NodeSeq]) = {
    var validItems	=	new ListBuffer[NodeSeq]
    for(item<-itemSeq){
      val message_id = item \\ "MessageId"
      if(!message_id.isEmpty && message_id.text.startsWith(message_namespace))
        validItems+=item
    }
    debug("validItems ="+validItems)
    //println("validItems ="+validItems)
    validItems
  }

  def message_namespace= "de"

  private def lastCallOf(method_name:String) = {
    var socketLabsApiCall:SocketLabsApiCall	=	null
    try{
      val session = HibernateUtil.factory.openSession();
      val list	=	session.createQuery("FROM SocketLabsApiCall slac where slac.methodName='"+method_name+"' ORDER BY slac.createdAt DESC").list()
      if(!list.isEmpty())
        socketLabsApiCall	=	list.get(0).asInstanceOf[SocketLabsApiCall];
      session.flush();
      session.close();
    }catch{
      case ex:Exception => error("Failed to execute lastCallOf : "+ex.getMessage())
      ex.printStackTrace()
    }
    socketLabsApiCall
  }

  private def calculateWindowArgs(socketLabsApiCall:SocketLabsApiCall):Map[String, String] ={
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
      else if(socketLabsApiCall.getHttpStatus()==null || socketLabsApiCall.getCount()==null)//previous call not fully processed
        windowArgsMap+=("startDate"->socketLabsApiCall.getStartDate().toString(),
          "endDate"->SocketLabsQueryService.today().toString(),
          "index"->socketLabsApiCall.getIndexVal().get.toString())
      else{
        if(!socketLabsApiCall.getStartDate().toString().isEmpty()){
          debug("default date : "+SocketLabsQueryService.defaultDate().toString())
          println("default date : "+SocketLabsQueryService.defaultDate().toString())
          start_date	=	SocketLabsQueryService.defaultDate().toString()
        }
        else
          start_date	=	socketLabsApiCall.getStartDate().toString()

        if((!socketLabsApiCall.getStartDate().toString().isEmpty()) &&
          (socketLabsApiCall.getEndDate().getTime()-socketLabsApiCall.getStartDate().getTime())>0 &&
          socketLabsApiCall.getCount().get<SocketLabsQueryService.max_items_returned_per_call){
          start_date	=	socketLabsApiCall.getEndDate().toString()
          index	=	"0";
        }else
          index	=	(socketLabsApiCall.getIndexVal().get + socketLabsApiCall.getCount().get).toString()
        windowArgsMap+=("startDate"->start_date,
          "endDate"->end_date,
          "index"->index)
      }
    }catch{
      case ex:Exception => error("Failed to calculate_window_params: "+ex.getMessage())
      ex.printStackTrace()
    }
    windowArgsMap
  }

}

object SocketLabsQueryService{
  val SOCKET_LABS_API_USER	=	"loyal3"
  val SOCKET_LABS_API_KEY	=	"d8f7d5b39ccb2d65b974"
  val SOCKET_LABS_API_HOST	=	"api.socketlabs.com"
  val SOCKET_LABS_API_PREFIX	=	"v1"
  val SOCKET_LABS_ACCOUNT_ID	=	"1143"
  val max_items_returned_per_call = 500

  def defaultDate():String = {
    val formatString = "yyyy-MM-dd"
    val cal = Calendar.getInstance
    cal.add(Calendar.DATE, -14)//today - 2.weeks
    println("default date time : "+cal.getTime())
    new SimpleDateFormat(formatString) format cal.getTime
  }

  def today():String = {
    val formatString = "yyyy-MM-dd"
    val cal = Calendar.getInstance
    new SimpleDateFormat(formatString) format cal.getTime
  }

  def main(args: Array[String]): Unit = {
    val queryParams	= Map("startDate"->"2011-02-18",
      "endDate"->"201",
      "index"->"0")
    val restClient	=	new SocketLabsQueryService
    restClient.runMessagesFailed()
    var socketLabsApiCall = new SocketLabsApiCall
    val id	=	com.loyal3.util.IdFactory.generateId
    println(id)
    socketLabsApiCall.setId(id)
    socketLabsApiCall.setStartDate(java.sql.Date.valueOf("2011-02-18"))
    socketLabsApiCall.setEndDate(java.sql.Date.valueOf("2011-12-18"))
    socketLabsApiCall.setIndexVal(Some(0))
    socketLabsApiCall.setMethodName("messagesFailed")
    //restClient.create_api_call("messagesFailed",queryParams)
  }
}
