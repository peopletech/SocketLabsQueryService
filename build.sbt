name := "SocketLabsQueryService"

resolvers += "TypeSafe MongoDB Casbah" at "http://repo.typesafe.com/typesafe/repo"

libraryDependencies += 
  "net.databinder" % "dispatch-http_2.9.1" % "0.8.8"

libraryDependencies += 
  "org.hibernate" % "hibernate-entitymanager" % "3.6.7.Final"
            
libraryDependencies += 
  "org.hibernate" % "hibernate-validator" % "4.2.0.Final"

//MySQL Connector 5.1.21
libraryDependencies += 
  "mysql" % "mysql-connector-java" % "5.1.17"

libraryDependencies += 
  "joda-time" % "joda-time" % "1.6.2"

libraryDependencies += 
  "joda-time" % "joda-time-hibernate" % "1.3"

libraryDependencies += 
  "org.scalatest" % "scalatest_2.9.1" % "1.8"
                        
libraryDependencies += 
  "net.liftweb" % "lift-webkit_2.9.1" % "2.4"

libraryDependencies +=
   "commons-httpclient" % "commons-httpclient" % "3.1"

libraryDependencies ++= Seq(
   "com.jolbox" % "bonecp" % "0.7.1.RELEASE",
   "com.jolbox" % "bonecp-provider" % "0.7.1.RELEASE",
   "commons-pool" % "commons-pool" % "1.6",
   "redis.clients" % "jedis" % "2.0.0")

libraryDependencies ++= Seq(
   "com.novus" % "salat_2.9.1" % "0.0.8")

libraryDependencies += 
   "com.mongodb.casbah" % "casbah_2.9.1" % "2.1.5-1"

libraryDependencies ++= Seq(
    "junit" % "junit" % "4.8.2",
    "org.scala-tools.testing" % "test-interface" % "0.5",
    "org.scalatest" %% "scalatest" % "1.6.1")

libraryDependencies += 
    "asm" % "asm-all" % "3.1"
