package loyal3.poc.test

import org.scalatest.FunSuite
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import loyal3.poc.SocketLabsQueryService
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.loyal3.model.email.SocketLabsApiCall
import loyal3.poc.utils.HibernateUtil
import scala.xml.XML
import com.loyal3.model.email.MongoEmailDAO
import com.mongodb.casbah.commons.MongoDBObject

@RunWith(classOf[JUnitRunner])
class SocketLabsQueryServiceTestSuite extends FunSuite {
	test("should construct the expected REST request") {
	  val socketLabsQueryService=	new SocketLabsQueryService
	  val method:String	=	"messagesFailed"
	  val queryParams	= Map("startDate"->"2012-09-27",
	      "endDate"->"2012-10-12",
	      "index"->"0")  
	  val expectedRequest	=	"https://api.socketlabs.com/v1/messagesFailed?accountId=1143&type=xml&index=0&endDate=2012-10-12&startDate=2012-09-27"
	  val req	=	socketLabsQueryService.constructRequest(method, queryParams)
	  assert(req.to_uri.toURL().toString()==expectedRequest)
	}
	
	test("should save the SocketLabsApiCall object to the database. http_status==null, count==null"){
	  var socketLabsApiCall:SocketLabsApiCall	=	null;
	  val socketLabsQueryService=	new SocketLabsQueryService
	  val method:String	=	"messagesFailed"
	  val queryParams	= Map("startDate"->"2012-09-30",
	      "endDate"->"2012-10-16",
	      "index"->"0")
	  socketLabsApiCall	=	socketLabsQueryService.createApiCall(method, queryParams)
	  try{
	      val session = HibernateUtil.factory.openSession();
	      val list	=	session.createQuery("FROM SocketLabsApiCall slac where slac.id='"+socketLabsApiCall.getId()+"' ORDER BY slac.createdAt DESC").list()
	      if(!list.isEmpty())
	        socketLabsApiCall	=	list.get(0).asInstanceOf[SocketLabsApiCall];
	      session.flush();
	      session.close();
	    }catch{
	      case ex:Exception => error("Failed to find socketLabsApiCall : "+ex.getMessage())
	      ex.printStackTrace()
	    }
	    assert(null!=socketLabsApiCall)
	  	assert(java.sql.Date.valueOf(queryParams.get("startDate").get)==socketLabsApiCall.getStartDate())
	  	assert(java.sql.Date.valueOf(queryParams.get("endDate").get)==socketLabsApiCall.getEndDate())
	  	assert(queryParams.get("index").get.toLong==socketLabsApiCall.getIndexVal().get)
	  	assert(null==socketLabsApiCall.getHttpStatus())
	  	assert(None==socketLabsApiCall.getCount())
	}
	
	test("should update the SocketLabsApiCall object to the database during a time out response. raw_response=null,http_status=null and count==0"){
		var socketLabsApiCall:SocketLabsApiCall	=	null;
	  val socketLabsQueryService=	new SocketLabsQueryService
	  val method:String	=	"messagesFailed"
	  val queryParams	= Map("startDate"->"2012-09-30",
	      "endDate"->"2012-10-16",
	      "index"->"0")
	  socketLabsApiCall	=	socketLabsQueryService.createApiCall(method, queryParams)
	  socketLabsQueryService.processApiResponse(socketLabsApiCall)
	  try{
	      val session = HibernateUtil.factory.openSession();
	      val list	=	session.createQuery("FROM SocketLabsApiCall slac where slac.id='"+socketLabsApiCall.getId()+"' ORDER BY slac.createdAt DESC").list()
	      if(!list.isEmpty())
	        socketLabsApiCall	=	list.get(0).asInstanceOf[SocketLabsApiCall];
	      session.flush();
	      session.close();
	    }catch{
	      case ex:Exception => error("Failed to find socketLabsApiCall : "+ex.getMessage())
	      ex.printStackTrace()
	    }
	    assert(null==socketLabsApiCall.getHttpStatus())
	  	assert(0==socketLabsApiCall.getCount().get)
	  	assert(null==socketLabsApiCall.getRawResponse())
	}
	
	test("should update the SocketLabsApiCall object to the database during an empty response. raw_response=null,http_status=200 and count==0"){
	  val socketLabsQueryService=	new SocketLabsQueryService
	  val method:String	=	"messagesFailed"
	  val queryParams	= Map("startDate"->"2013-10-27",
	      "endDate"->"2013-11-12",
	      "index"->"0")  
	  var socketLabsApiCall:SocketLabsApiCall	=	socketLabsQueryService.createApiCall(method, queryParams)
	  socketLabsApiCall	=	socketLabsQueryService.performApiCall(method, queryParams, socketLabsApiCall)
	  socketLabsQueryService.processApiResponse(socketLabsApiCall)
	  try{
	      val session = HibernateUtil.factory.openSession();
	      val list	=	session.createQuery("FROM SocketLabsApiCall slac where slac.id='"+socketLabsApiCall.getId()+"' ORDER BY slac.createdAt DESC").list()
	      if(!list.isEmpty())
	        socketLabsApiCall	=	list.get(0).asInstanceOf[SocketLabsApiCall];
	      session.flush();
	      session.close();
	    }catch{
	      case ex:Exception => error("Failed to find socketLabsApiCall : "+ex.getMessage())
	      ex.printStackTrace()
	    }
	    assert("200"==socketLabsApiCall.getHttpStatus())
	  	assert(None==socketLabsApiCall.getCount())
	  	assert(null!=socketLabsApiCall.getRawResponse())
	}
	
	val xml_resp_with_1_valid_item	=	"<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?><response><timestamp>2011-07-29T23:57:15.4038984Z</timestamp><totalCount>1390</totalCount><count>3</count><collection>		<item>			<ServerId>1185</ServerId>			<DateTime>2011-07-18T18:08:39Z</DateTime>			<MessageId>de3j706y6a3p4z2m2f625o5w6g724b5n5u</MessageId>			<MailingId/>			<ToAddress>test1308158192-797436@example.org</ToAddress>			<FromAddress>do-not-reply-test@loyal3.com</FromAddress>			<FailureType>0</FailureType>			<FailureCode>4003</FailureCode>			<Reason>500 5.4.0 System rule action set to fail connection.</Reason>		</item>    <item>			<ServerId>1185</ServerId>			<DateTime>2011-07-26T18:42:33Z</DateTime>			<MessageId>s-6q3s6a5727125u152f2x5r1f345b051g</MessageId>			<MailingId/>			<ToAddress>dummy+david+cc2011May1_1514_loyal3.com@loyal3.com</ToAddress>			<FromAddress>do-not-reply-staging@loyal3.com</FromAddress>			<FailureType>2</FailureType>			<FailureCode>9999</FailureCode>			<Reason>NA</Reason>		</item>		<item>			<ServerId>1185</ServerId>			<DateTime>2011-07-19T11:02:09Z</DateTime>			<MessageId/>			<MailingId/>			<ToAddress>root@stageapp02.loyal3.com</ToAddress>			<FromAddress>root@stageapp02.loyal3.com</FromAddress>			<FailureType>1</FailureType>			<FailureCode>2002</FailureCode>			<Reason>Name service error for stageapp02.loyal3.com Type=A: Host not found</Reason>		</item></collection></response>"
	test("Failed Messages extraction. should extract only valid items"){
	  val socketLabsQueryService=	new SocketLabsQueryService
	  val responseXml: scala.xml.Elem	=	XML.loadString(xml_resp_with_1_valid_item)
	  val valid_item_count	=	socketLabsQueryService.extractValidItems(responseXml)
	  //println("item_count="+valid_item_count)
	  assert(1==valid_item_count.size)
	}
	
	val xml_resp_large_set	=	"<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?><response><timestamp>2011-08-01T23:14:26.6549355Z</timestamp><totalCount>50</totalCount><count>50</count><collection><item><ServerId>1185</ServerId><DateTime>2011-07-18T11:02:07Z</DateTime><MessageId></MessageId><MailingId></MailingId><ToAddress>root@stageapp02.loyal3.com</ToAddress><FromAddress>root@stageapp02.loyal3.com</FromAddress><FailureType>1</FailureType><FailureCode>2002</FailureCode><Reason>Name service error for stageapp02.loyal3.com Type=A: Host not found</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-18T16:29:04Z</DateTime><MessageId>t-6p3i5e3u5c226e081w2c3a0a3e182e2g</MessageId><MailingId></MailingId><ToAddress>dummy+brandon.ellis_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-18T18:08:39Z</DateTime><MessageId>art-3j706y6a3p4z2m2f625o5w6g724b5n5u</MessageId><MailingId></MailingId><ToAddress>test1308158192-797436@example.org</ToAddress><FromAddress>do-not-reply-test@loyal3.com</FromAddress><FailureType>0</FailureType><FailureCode>4003</FailureCode><Reason>500 5.4.0 System rule action set to fail connection.</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-19T11:02:09Z</DateTime><MessageId></MessageId><MailingId></MailingId><ToAddress>root@stageapp02.loyal3.com</ToAddress><FromAddress>root@stageapp02.loyal3.com</FromAddress><FailureType>1</FailureType><FailureCode>2002</FailureCode><Reason>Name service error for stageapp02.loyal3.com Type=A: Host not found</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-21T11:02:01Z</DateTime><MessageId></MessageId><MailingId></MailingId><ToAddress>root@stageapp01.loyal3.com</ToAddress><FromAddress>root@stageapp01.loyal3.com</FromAddress><FailureType>1</FailureType><FailureCode>2002</FailureCode><Reason>Name service error for stageapp01.loyal3.com Type=A: Host not found</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-22T11:01:56Z</DateTime><MessageId></MessageId><MailingId></MailingId><ToAddress>root@stageapp01.loyal3.com</ToAddress><FromAddress>root@stageapp01.loyal3.com</FromAddress><FailureType>1</FailureType><FailureCode>2002</FailureCode><Reason>Name service error for stageapp01.loyal3.com Type=A: Host not found</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-23T11:01:47Z</DateTime><MessageId></MessageId><MailingId></MailingId><ToAddress>root@stageapp01.loyal3.com</ToAddress><FromAddress>root@stageapp01.loyal3.com</FromAddress><FailureType>1</FailureType><FailureCode>2002</FailureCode><Reason>Name service error for stageapp01.loyal3.com Type=A: Host not found</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-24T11:01:25Z</DateTime><MessageId></MessageId><MailingId></MailingId><ToAddress>root@stageapp01.loyal3.com</ToAddress><FromAddress>root@stageapp01.loyal3.com</FromAddress><FailureType>1</FailureType><FailureCode>2002</FailureCode><Reason>Name service error for stageapp01.loyal3.com Type=A: Host not found</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-25T11:01:17Z</DateTime><MessageId></MessageId><MailingId></MailingId><ToAddress>root@stageapp01.loyal3.com</ToAddress><FromAddress>root@stageapp01.loyal3.com</FromAddress><FailureType>1</FailureType><FailureCode>2002</FailureCode><Reason>Name service error for stageapp01.loyal3.com Type=A: Host not found</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-26T01:27:53Z</DateTime><MessageId>s-0e4r3w1w3e5u4h391n47166s0e623d38</MessageId><MailingId></MailingId><ToAddress>dummy+buddhabud88_gmail.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-26T11:01:09Z</DateTime><MessageId></MessageId><MailingId></MailingId><ToAddress>root@stageapp01.loyal3.com</ToAddress><FromAddress>root@stageapp01.loyal3.com</FromAddress><FailureType>1</FailureType><FailureCode>2002</FailureCode><Reason>Name service error for stageapp01.loyal3.com Type=A: Host not found</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-26T18:42:29Z</DateTime><MessageId>s-201z715g112a5n6j5p6v0a23582h4x4g</MessageId><MailingId></MailingId><ToAddress>dummy+david+cc2011May12_1455_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-26T18:42:33Z</DateTime><MessageId>s-075v473v2g3k1v1l41323l3b2o5f440t</MessageId><MailingId></MailingId><ToAddress>dummy+stephen_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-26T18:42:33Z</DateTime><MessageId>s-6q3s6a5727125u152f2x5r1f345b051g</MessageId><MailingId></MailingId><ToAddress>dummy+david+cc2011May1_1514_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-26T18:42:34Z</DateTime><MessageId>s-713y6z3d215p0t6i2h5d160u2k6t5d21</MessageId><MailingId></MailingId><ToAddress>dummy+matt+05.12.04_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-26T18:42:36Z</DateTime><MessageId>s-6t5o534f2m4p7350465k1i1z5g3q2n3v</MessageId><MailingId></MailingId><ToAddress>dummy+tyber+st_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-26T18:42:36Z</DateTime><MessageId>s-1g1j1g73452u6a525q1o4s1c053m0r1k</MessageId><MailingId></MailingId><ToAddress>dummy+david+stest_2011JUN01_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-26T18:42:37Z</DateTime><MessageId>s-613o6521606f6v3g705a1o370t1z191g</MessageId><MailingId></MailingId><ToAddress>dummy+tyber+st_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-26T18:42:39Z</DateTime><MessageId>s-6q1d4t2b5m6v0h2p0c3j3j4i193k453p</MessageId><MailingId></MailingId><ToAddress>dummy+matt+05.14.01_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-26T18:42:44Z</DateTime><MessageId>s-3p1w4r204a0w114p0s325u260k0y505s</MessageId><MailingId></MailingId><ToAddress>dummy+matt+05.12.03_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T06:59:52Z</DateTime><MessageId>s-3i6g645b3v2x1423314p2m6w2k5r366d</MessageId><MailingId></MailingId><ToAddress>dummy+bad-email_beefdonut.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T06:59:52Z</DateTime><MessageId>s-494d1o294d1a606z6i3q1d724l0f5q6y</MessageId><MailingId></MailingId><ToAddress>dummy+matt+30_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T06:59:53Z</DateTime><MessageId>s-356q0t092r2r2j472c0t5t0a2e5c5q67</MessageId><MailingId></MailingId><ToAddress>dummy+david+cc2011May12_1455_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T06:59:53Z</DateTime><MessageId>s-4b633s1b062c4i6s3a0e6w2a1d2o2v6o</MessageId><MailingId></MailingId><ToAddress>dummy+david+cc2011May1_1514_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T06:59:53Z</DateTime><MessageId>s-1a3m1v460k2d2a7062366g194q0c0n2h</MessageId><MailingId></MailingId><ToAddress>dummy+matt+05.12.03_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T06:59:54Z</DateTime><MessageId>s-722k3p1d150m0y6s5r0n6a1a5x000g5f</MessageId><MailingId></MailingId><ToAddress>dummy+stephen_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T06:59:54Z</DateTime><MessageId>s-254x626p6p1x0e5y5o6w1z41031g1944</MessageId><MailingId></MailingId><ToAddress>dummy+matt+05.12.04_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T06:59:54Z</DateTime><MessageId>s-294c5t0a27604x563a2k3w0n3l3e2d3u</MessageId><MailingId></MailingId><ToAddress>dummy+tyber+st_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T06:59:54Z</DateTime><MessageId>s-2d0l465x3n5e722v2p4h3r0u243a3c1u</MessageId><MailingId></MailingId><ToAddress>dummy+tyber+st_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T06:59:55Z</DateTime><MessageId>s-5q3b3f5i1d3i3z4l6l4t6t2e042h3g62</MessageId><MailingId></MailingId><ToAddress>dummy+matt+05.14.01_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T06:59:55Z</DateTime><MessageId>s-51164g716v37305s2p4p384q6b1r2p0j</MessageId><MailingId></MailingId><ToAddress>dummy+han+0518b_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T06:59:55Z</DateTime><MessageId>s-2k1a033z0r301g0b1o1c6e6321463n0l</MessageId><MailingId></MailingId><ToAddress>dummy+han+0520t_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T06:59:56Z</DateTime><MessageId>s-5m2173451w5m644s441e4c190d3q5n19</MessageId><MailingId></MailingId><ToAddress>dummy+han+0521b_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T06:59:58Z</DateTime><MessageId>s-1y0m4k5q3201144x382c136m3l5t385w</MessageId><MailingId></MailingId><ToAddress>dummy+han+0524b_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T06:59:59Z</DateTime><MessageId>s-6d6h7224092b6b495b45074w2u314j5x</MessageId><MailingId></MailingId><ToAddress>dummy+Don_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T07:00:00Z</DateTime><MessageId>s-6i3r013q2n6y5r4y5t042g5x071h3y1y</MessageId><MailingId></MailingId><ToAddress>dummy+han+0525b_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T07:00:01Z</DateTime><MessageId>s-5z18573n4g4o1r6q585x2m446v4g1369</MessageId><MailingId></MailingId><ToAddress>dummy+han+0601_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T07:00:01Z</DateTime><MessageId>s-6j492k1s2i0g43665w6m0p441q125c0i</MessageId><MailingId></MailingId><ToAddress>dummy+han+0601_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T07:00:02Z</DateTime><MessageId>s-3f1t512b1y4c0q472o0q4p3p6h3l6i3f</MessageId><MailingId></MailingId><ToAddress>dummy+david+stest_2011JUN01_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T07:00:04Z</DateTime><MessageId>s-4t6e513v6l2q1o3n0f2i664l4j3j4a4i</MessageId><MailingId></MailingId><ToAddress>dummy+tyber+yak_gmail.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T07:00:05Z</DateTime><MessageId>s-2k287235635b5m5x421e6s4c2m2y0u1h</MessageId><MailingId></MailingId><ToAddress>dummy+tyber+nop_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T07:00:06Z</DateTime><MessageId>s-2s4a3o4m1v4p4r5h3s4q6g0d6j41482l</MessageId><MailingId></MailingId><ToAddress>dummy+han+cc3_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T07:00:10Z</DateTime><MessageId>s-1e4z04291l5a2t0z5h3y682a0l411a17</MessageId><MailingId></MailingId><ToAddress>dummy+tyberlesjack_aol.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T07:00:18Z</DateTime><MessageId>s-064i11096r4z3p4p38343n6g395f1114</MessageId><MailingId></MailingId><ToAddress>dummy+han+0608_loyal3.com@loyal3.com</ToAddress><FromAddress>do-not-reply-staging@loyal3.com</FromAddress><FailureType>2</FailureType><FailureCode>9999</FailureCode><Reason>NA</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-27T11:01:03Z</DateTime><MessageId></MessageId><MailingId></MailingId><ToAddress>root@stageapp01.loyal3.com</ToAddress><FromAddress>root@stageapp01.loyal3.com</FromAddress><FailureType>1</FailureType><FailureCode>2002</FailureCode><Reason>Name service error for stageapp01.loyal3.com Type=A: Host not found</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-28T11:00:53Z</DateTime><MessageId></MessageId><MailingId></MailingId><ToAddress>root@stageapp01.loyal3.com</ToAddress><FromAddress>root@stageapp01.loyal3.com</FromAddress><FailureType>1</FailureType><FailureCode>2002</FailureCode><Reason>Name service error for stageapp01.loyal3.com Type=A: Host not found</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-29T11:00:47Z</DateTime><MessageId></MessageId><MailingId></MailingId><ToAddress>root@stageapp01.loyal3.com</ToAddress><FromAddress>root@stageapp01.loyal3.com</FromAddress><FailureType>1</FailureType><FailureCode>2002</FailureCode><Reason>Name service error for stageapp01.loyal3.com Type=A: Host not found</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-30T11:02:24Z</DateTime><MessageId></MessageId><MailingId></MailingId><ToAddress>root@stageapp01.loyal3.com</ToAddress><FromAddress>root@stageapp01.loyal3.com</FromAddress><FailureType>1</FailureType><FailureCode>2002</FailureCode><Reason>Name service error for stageapp01.loyal3.com Type=A: Host not found</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-07-31T11:02:12Z</DateTime><MessageId></MessageId><MailingId></MailingId><ToAddress>root@stageapp01.loyal3.com</ToAddress><FromAddress>root@stageapp01.loyal3.com</FromAddress><FailureType>1</FailureType><FailureCode>2002</FailureCode><Reason>Name service error for stageapp01.loyal3.com Type=A: Host not found</Reason></item><item><ServerId>1185</ServerId><DateTime>2011-08-01T11:02:10Z</DateTime><MessageId></MessageId><MailingId></MailingId><ToAddress>root@stageapp01.loyal3.com</ToAddress><FromAddress>root@stageapp01.loyal3.com</FromAddress><FailureType>1</FailureType><FailureCode>2002</FailureCode><Reason>Name service error for stageapp01.loyal3.com Type=A: Host not found</Reason></item></collection></response>"
	test("Failed Messages extraction. should extract only valid items from a large set"){
	  val socketLabsQueryService=	new SocketLabsQueryService
	  val responseXml: scala.xml.Elem	=	XML.loadString(xml_resp_with_1_valid_item)
	  val valid_item_count	=	socketLabsQueryService.extractValidItems(responseXml)
	  //println("item_count="+valid_item_count)
	  assert(1==valid_item_count.size)
	}
	
	test("Failed Messages extraction.should handle error responses and ignore ones that do not have correct message_id"){
	  val socketLabsQueryService=	new SocketLabsQueryService
	  val method:String	=	"messagesFailed"
	  val queryParams	= Map("startDate"->"2013-10-27",
	      "endDate"->"2013-11-12",
	      "index"->"0")  
	  var socketLabsApiCall:SocketLabsApiCall	=	socketLabsQueryService.createApiCall(method, queryParams)
	  socketLabsApiCall.setRawResponse(xml_resp_with_1_valid_item)
	  socketLabsApiCall.setHttpStatus("200")
	  socketLabsQueryService.processApiResponse(socketLabsApiCall)
	  try{
	      val session = HibernateUtil.factory.openSession();
	      val list	=	session.createQuery("FROM SocketLabsApiCall slac where slac.id='"+socketLabsApiCall.getId()+"' ORDER BY slac.createdAt DESC").list()
	      if(!list.isEmpty())
	        socketLabsApiCall	=	list.get(0).asInstanceOf[SocketLabsApiCall];
	      session.flush();
	      session.close();
	    }catch{
	      case ex:Exception => error("Failed to find socketLabsApiCall : "+ex.getMessage())
	      ex.printStackTrace()
	    }
	    assert(3==socketLabsApiCall.getCount().get)
	}
	
	val xml_resp_one_invalid = "<response>	<timestamp>2011-08-16T13:22:27.2654178Z</timestamp>	<totalCount>1</totalCount>	<count>1</count>	<collection>		<item>			<ServerId>1185</ServerId>			<DateTime>2011-08-15T11:01:06Z</DateTime>			<MessageId/>			<MailingId/>			<ToAddress>root@stageapp01.loyal3.com</ToAddress>			<FromAddress>root@stageapp01.loyal3.com</FromAddress>			<FailureType>1</FailureType>			<FailureCode>2002</FailureCode>			<Reason>Name service error for stageapp01.loyal3.com Type=A: Host not found</Reason>		</item>	</collection></response>"
	test("Failed Messages extraction.should handle one failed  ONE invalid item"){
	  val socketLabsQueryService=	new SocketLabsQueryService
	  val responseXml: scala.xml.Elem	=	XML.loadString(xml_resp_one_invalid)
	  val valid_item_count	=	socketLabsQueryService.extractValidItems(responseXml)
	  //println("item_count="+valid_item_count)
	  assert(0==valid_item_count.size)
	}
	
	 
	test("calculate window params.previous call had same start and end date, in the past and previous call had 0 index and 0 count"){
	  Thread.sleep(1000)
	  var socketLabsApiCall:SocketLabsApiCall	=	null;
	  val socketLabsQueryService=	new SocketLabsQueryService
	  val method:String	=	"messagesFailed"
	  val queryParams	= Map("startDate"->SocketLabsQueryService.defaultDate(),
	      "endDate"->SocketLabsQueryService.defaultDate(),
	      "index"->"0")
	  socketLabsApiCall	=	socketLabsQueryService.createApiCall(method, queryParams)
	  socketLabsApiCall.setCount(Some(0))
	  socketLabsQueryService.updateAttributes(socketLabsApiCall)
	  socketLabsApiCall	=	socketLabsQueryService.lastCallOf(method)
	  val windowArgsMap	=	socketLabsQueryService.calculateWindowArgs(socketLabsApiCall)
	  assert(3==windowArgsMap.size)
	  assert(java.sql.Date.valueOf(windowArgsMap.get("startDate").get)==socketLabsApiCall.getEndDate())
	  assert(java.sql.Date.valueOf(windowArgsMap.get("endDate").get)==java.sql.Date.valueOf(SocketLabsQueryService.today()))
	  assert(0==socketLabsApiCall.getIndexVal().get)
	  
	}
	
	 
	test("calculate window params.previous call had same start and end date, in the past and previous call had 0 index and non-zero count less than max"){
	  Thread.sleep(1000)
	  var socketLabsApiCall:SocketLabsApiCall	=	null;
	  val socketLabsQueryService=	new SocketLabsQueryService
	  val method:String	=	"messagesFailed"
	  val queryParams	= Map("startDate"->SocketLabsQueryService.defaultDate(),
	      "endDate"->SocketLabsQueryService.defaultDate(),
	      "index"->"0")
	  socketLabsApiCall	=	socketLabsQueryService.createApiCall(method, queryParams)
	  socketLabsApiCall.setCount(Some(10))
	  socketLabsApiCall.setHttpStatus("200")
	  socketLabsQueryService.updateAttributes(socketLabsApiCall)
	  socketLabsApiCall	=	socketLabsQueryService.lastCallOf(method)
	  val windowArgsMap	=	socketLabsQueryService.calculateWindowArgs(socketLabsApiCall)
	  assert(3==windowArgsMap.size)
	  assert(java.sql.Date.valueOf(windowArgsMap.get("startDate").get)==socketLabsApiCall.getEndDate())
	  assert(java.sql.Date.valueOf(windowArgsMap.get("endDate").get)==java.sql.Date.valueOf(SocketLabsQueryService.today()))
	  assert(10==windowArgsMap.get("index").get.toInt)
	}
	
  
	test("calculate window params.previous call had same start and end date, in the past and previous call had 0 index and non-zero count equal to max"){
	  Thread.sleep(1000)
	   var socketLabsApiCall:SocketLabsApiCall	=	null;
	  val socketLabsQueryService=	new SocketLabsQueryService
	  val method:String	=	"messagesFailed"
	  val queryParams	= Map("startDate"->SocketLabsQueryService.defaultDate(),
	      "endDate"->SocketLabsQueryService.defaultDate(),
	      "index"->"0")
	  socketLabsApiCall	=	socketLabsQueryService.createApiCall(method, queryParams)
	  socketLabsApiCall.setCount(Some(500))
	  socketLabsApiCall.setHttpStatus("200")
	  socketLabsQueryService.updateAttributes(socketLabsApiCall)
	  socketLabsApiCall	=	socketLabsQueryService.lastCallOf(method)
	  val windowArgsMap	=	socketLabsQueryService.calculateWindowArgs(socketLabsApiCall)
	  assert(3==windowArgsMap.size)
	  assert(java.sql.Date.valueOf(windowArgsMap.get("startDate").get)==socketLabsApiCall.getEndDate())
	  assert(java.sql.Date.valueOf(windowArgsMap.get("endDate").get)==java.sql.Date.valueOf(SocketLabsQueryService.today()))
	  assert(500==windowArgsMap.get("index").get.toInt)
	}
	
	test("calculate window params.previous call had same start and end date, in the past and previous call had non-zero index and 0 count"){
	  Thread.sleep(1000)
	   var socketLabsApiCall:SocketLabsApiCall	=	null;
	  val socketLabsQueryService=	new SocketLabsQueryService
	  val method:String	=	"messagesFailed"
	  val queryParams	= Map("startDate"->SocketLabsQueryService.defaultDate(),
	      "endDate"->SocketLabsQueryService.defaultDate(),
	      "index"->"10")
	  socketLabsApiCall	=	socketLabsQueryService.createApiCall(method, queryParams)
	  socketLabsApiCall	=	socketLabsQueryService.lastCallOf(method)
	  val windowArgsMap	=	socketLabsQueryService.calculateWindowArgs(socketLabsApiCall)
	  assert(3==windowArgsMap.size)
	  assert(java.sql.Date.valueOf(windowArgsMap.get("startDate").get)==socketLabsApiCall.getStartDate())
	  assert(java.sql.Date.valueOf(windowArgsMap.get("endDate").get)==java.sql.Date.valueOf(SocketLabsQueryService.today()))
	  assert(10==windowArgsMap.get("index").get.toInt)
	}
	
	test("calculate window params.previous call had same start and end date, in the past and previous call had non-zero index and non-zero count"){
	  Thread.sleep(1000)
	  var socketLabsApiCall:SocketLabsApiCall	=	null;
	  val socketLabsQueryService=	new SocketLabsQueryService
	  val method:String	=	"messagesFailed"
	  val queryParams	= Map("startDate"->SocketLabsQueryService.defaultDate(),
	      "endDate"->SocketLabsQueryService.defaultDate(),
	      "index"->"10")
	  socketLabsApiCall	=	socketLabsQueryService.createApiCall(method, queryParams)
	  socketLabsApiCall.setCount(Some(10))
	  socketLabsApiCall.setHttpStatus("200")
	  socketLabsQueryService.updateAttributes(socketLabsApiCall)
	  socketLabsApiCall	=	socketLabsQueryService.lastCallOf(method)
	  val windowArgsMap	=	socketLabsQueryService.calculateWindowArgs(socketLabsApiCall)
	  assert(3==windowArgsMap.size)
	  assert(java.sql.Date.valueOf(windowArgsMap.get("startDate").get)==socketLabsApiCall.getStartDate())
	  assert(java.sql.Date.valueOf(windowArgsMap.get("endDate").get)==java.sql.Date.valueOf(SocketLabsQueryService.today()))
	  assert(20==windowArgsMap.get("index").get.toInt)
	}
	
	test("calculate window params.previous call had same start and end date, in the past and previous call had non-zero index and max count"){
	  Thread.sleep(1000)
	  var socketLabsApiCall:SocketLabsApiCall	=	null;
	  val socketLabsQueryService=	new SocketLabsQueryService
	  val method:String	=	"messagesFailed"
	  val queryParams	= Map("startDate"->SocketLabsQueryService.defaultDate(),
	      "endDate"->SocketLabsQueryService.defaultDate(),
	      "index"->"10")
	  socketLabsApiCall	=	socketLabsQueryService.createApiCall(method, queryParams)
	  socketLabsApiCall.setCount(Some(500))
	  socketLabsApiCall.setHttpStatus("200")
	  socketLabsQueryService.updateAttributes(socketLabsApiCall)
	  socketLabsApiCall	=	socketLabsQueryService.lastCallOf(method)
	  val windowArgsMap	=	socketLabsQueryService.calculateWindowArgs(socketLabsApiCall)
	  assert(3==windowArgsMap.size)
	  assert(java.sql.Date.valueOf(windowArgsMap.get("startDate").get)==socketLabsApiCall.getStartDate())
	  assert(java.sql.Date.valueOf(windowArgsMap.get("endDate").get)==java.sql.Date.valueOf(SocketLabsQueryService.today()))
	  assert(510==windowArgsMap.get("index").get.toInt)
	}
	
	val raw_response="<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?><response><timestamp>2010-02-02T01:34:42.1481507Z</timestamp><count>2</count><collection><item><AccountId>1143</AccountId><DateTime>2010-02-01T03:16:43Z</DateTime><MessageId>de6q3s6a5727125u152f2x5r1f345b051g</MessageId><MailingId></MailingId><ToAddress>bob_bounce1@example.com</ToAddress><FromAddress>admin@loyal3.com</FromAddress><FailureType>0</FailureType></item><item><AccountId>1143</AccountId><DateTime>2010-02-01T03:16:46Z</DateTime><MessageId>de6q3s6a8407125u920f2x5r1f345b051g</MessageId><MailingId></MailingId><ToAddress>junk_junk_junk@loyal3.com</ToAddress><FromAddress>admin2@loyal3.com</FromAddress><FailureType>1</FailureType></item></collection></response>"
	test("should add bounced_email records"){
	  Thread.sleep(1000)
	  var socketLabsApiCall:SocketLabsApiCall	=	null;
	  val socketLabsQueryService=	new SocketLabsQueryService
	  val method:String	=	"messagesFailed"
	  val queryParams	= Map("startDate"->SocketLabsQueryService.defaultDate(),
	      "endDate"->SocketLabsQueryService.defaultDate(),
	      "index"->"0")
	  socketLabsApiCall	=	socketLabsQueryService.createApiCall(method, queryParams)
	  socketLabsApiCall.setRawResponse(raw_response)
	  socketLabsApiCall.setHttpStatus("200")
	  val mongoEmailList	=	socketLabsQueryService.processApiResponse(socketLabsApiCall)
	  assert(null!=mongoEmailList)
	  assert(mongoEmailList.size==2)
	  for(i<- 0 to mongoEmailList.size-1){
	    val mongoEmail	=	MongoEmailDAO.findOne(MongoDBObject("user_id" -> mongoEmailList(i).user_id)).get
	    assert(mongoEmail.user_id==mongoEmailList(i).user_id)
	  }
	  
	  
	}
	
}