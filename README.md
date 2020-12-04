Individuals Business End of Period Statement API
========================
The Individuals Business End of Period Statement API allows software packages to:

```
· to submit a declaration that the submission data for a business is complete. 
```

## Requirements
- Scala 2.12.x
- Java 8
- sbt 1.3.13
- [Service Manager](https://github.com/hmrc/service-manager)

## Development Setup
To run the microservice from console, use `sbt run`. (starts on port 7785)

To start the service manager profile: `sm --start MTDFB_BUSINESS_EOPS`.
 
## Run Tests
```
sbt test
sbt it:test
```

## To view the RAML

Start api definition services

```
sm --start COMBINED_API_DEFINITION API_DEFINITION API_EXAMPLE_MICROSERVICE API_DOCUMENTATION_FRONTEND -f
sm --start ASSETS_FRONTEND -r 3.11.0 -f
```

Go to http://localhost:9680/api-documentation/docs/api/preview and enter http://localhost:7785/api/conf/1.0/application.raml 

## Documentation
To view documentation locally run the following service manager profile:
```
MTDFB_BUSINESS_EOPS
```
and run api-documentation-frontend:
```
./run_local_with_dependencies.sh
```
then go to http://localhost:9680/api-documentation/docs/api/preview and using this port and version:
```
http://localhost:7785/api/conf/1.0/application.raml
```

## Reporting Issues
You can create a GitHub issue [here](https://github.com/hmrc/individuals-expenses-api/issues)

## License
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
