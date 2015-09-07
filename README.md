jersey-planner-java-maven
=============================================

Planner REST Service.

## Language
- [Java][1]

## APIs
- [Amazon EC2][2]
- [Jersey][3]

## Tools
- [Tomcat][4]
- [Maven][5]

## Setup Instructions

1. Run the application with `mvn -X tomcat7:run`, and ensure it's
   running by visiting your local server's address (by default
   [localhost:8080][6].)

1. Deploy your application to Google App Engine with

   $ mvn tomcat7:deploy
   $ mvn tomcat7:redeploy

[1]: http://java.com/en/
[2]: https://aws.amazon.com/ec2
[3]: https://jersey.java.net/
[4]: http://tomcat.apache.org/
[5]: https://maven.apache.org/
[6]: https://localhost:8080/
