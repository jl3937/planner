jersey-planner-java-maven
=============================================

Planner REST Service.

## Language
- [Java][1]

## APIs
- [Jersey][2]

## Tools
- [Amazon EC2][3]
- [Tomcat][4]
- [Maven][5]

## Setup Instructions

1. Run the application with `mvn -X tomcat7:run`, and ensure it's
   running by visiting your local server's address (by default
   [localhost:8080/planner][6].)

1. Send a test POST request

   $ curl -X POST http://localhost:8080/planner/rest/plan -d '{"event":[{"content":"mission impossible", "type":"MOVIE"},{"content":"sushi","type":"FOOD"}],"requirement":{"start_loc":"94404","travel_mode":"DRIVING"}}'

1. Deploy your application to Tomcat with

   $ mvn tomcat7:deploy or $ mvn tomcat7:redeploy

[1]: http://java.com/en/
[2]: https://jersey.java.net/
[3]: https://aws.amazon.com/ec2
[4]: http://tomcat.apache.org/
[5]: https://maven.apache.org/
[6]: http://localhost:8080/planner
