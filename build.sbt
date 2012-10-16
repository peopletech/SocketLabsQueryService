name := "SocketLabsQueryService"

libraryDependencies += 
  "net.databinder" %% "dispatch-http" % "0.8.8"

libraryDependencies += 
  "org.hibernate" % "hibernate-entitymanager" % "3.6.7.Final"
            
libraryDependencies += 
  "org.hibernate" % "hibernate-validator" % "4.2.0.Final"

//MySQL Connector 5.1.21
libraryDependencies += 
  "mysql" % "mysql-connector-java" % "5.1.21"

libraryDependencies += 
  "joda-time" % "joda-time" % "1.6.2"

libraryDependencies += 
  "joda-time" % "joda-time-hibernate" % "1.3"

libraryDependencies += 
  "org.scalatest" % "scalatest_2.9.1" % "1.8"
            
libraryDependencies += 
  "com.novus" % "salat-core_2.9.1" % "1.9.1"
            
libraryDependencies += 
  "net.liftweb" % "lift-webkit_2.9.1" % "2.4"

libraryDependencies +=
   "commons-httpclient" % "commons-httpclient" % "3.1"

libraryDependencies ++= Seq(
   "com.jolbox" % "bonecp" % "0.7.1.RELEASE",
   "com.jolbox" % "bonecp-provider" % "0.7.1.RELEASE",
   "commons-pool" % "commons-pool" % "1.6",
   "redis.clients" % "jedis" % "2.0.0")

 
