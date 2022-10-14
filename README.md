# QA Debugging API Tests


## Prereqs
1)`brew install gradle`
2) If you're using IntelliJ you can set up your project and download java 17/kotlin during the setup steps and do not need to do it independently


## Setup (IntelliJ)
1) JDK 17 (be sure project SDK is also 17)
2) Gradle Plugin
3) Kotlin Plugin
4) If you would like your tests to run in IntelliJ when you click the green play button, Settings -> Build -> Build Tools -> Gradle -> `Run Tests Using` -> `IntelliJ IDEA` 


## Running

### IntelliJ
1) build with gradle (either through gradle plugin or through build menu item)
2) Navigate to the test class `QADDebuggingTests` and click on the green play button either on the class or on the individual tests

### CommandLine 
To just build (skip the tests -- this is useful because there are failing tests so the build will fail because tests run as part of the build by default)
```shell
./gradlew build -x test
```

To run the tests: (currently there are failing tests - intentional)
```shell
./gradlew test

```

## Libraries 
- OKHttp - Square's HTTP Client 
- Lettuce for REDIS 
- GSON - for parsing JSON content
- Junit for assertions
- Dotenv for env variable management (although done poorly at this moment)
- Also, as an FYI I scaffolded the generic setup to get a base project with things already populated like folder structure/.gitignore etc. 

## Etc
- Kotlin 
- Java 17 compile/runtime  - SDK/JDK
- Gradle (kts)

## Things that should be improved 
- Test Tagging - useful for targeting tests - think like on CI and you want to run some tests at some times depending on what you want to test. Example: Tests tagged `ServiceA` exercise ServiceA in some capacity, so it may be beneficial for when changes are made to ServiceA, that those tests are run. An example here would be tests that we know are going to fail as well as the performance one. 
- This one I have mixed feelings on : fluent assertions. - While they're beautiful sometimes they can be tricky to debug depending on implementation. There is likely a happy implementation somewhere in the middle, so we can check multiple items in a well-formed response body 
- dotenv should be less haphazard/ overall env vars handled more elegantly 
- follow up to dotenv - also REDIS should not take those values in as a constructor - if dotenv were handled more elegantly they would not need to passed in that way
- follow up to dotenv again - in all likelihood something like the REDIS_PASSWORD or the API_TOKEN would be stored somewhere in a secretsmanager like location, so there probably should be code to handle that request within this project (perhaps using an internal or external library) in addition to somehow being 'logged in' on the machine to pass the profile. On CI that would be translate-able.
- HTTP client should exist under `clients` on a per client basis and should house the individual clients 
- Code needs to be DRY'd up - there is significant repetition 

## Mixed Opinions
- generating a UUID and just hoping it will work for our use case. The numbers are on our side here that in theory we should never run into a duplicate, but we may want to revisit that implementation by checking to ensure the generated UUID does not already exist as a key

## Test Assertions
- - JUnit

## Bugs

POST request should return a 404 when the key passed into the request already exists in REDIS - Currently it returns a 200.

 Test to see this: `postKeyAlreadyExists`

To reproduce: 
1) Connect to redis-cli 
2) Either: 
- get all the keys and view them to ensure the key you want to add is not already there 
```keys *```
- forcibly attempt to delete the key you want to create to ensure it does not exist.
```DEL <desired_key>```
3) issue a POST request to create that key with a value 
```shell
curl --location --request POST 'https://qa-debugging-exercise.herokuapp.com/keys?name=<DESIRED_KEY>&value=<DESIRED_VALUE>' \
--header 'token: QLNoqW87JWVjdTEn'
```
4) Sanity check: go back to redis-cli to ensure that the key/value pair combination was actually created. 
- ```keys *``` (and it should be included in that list)
- ``` GET <KEY_NAME>``` should return the value 
5) issue the same exact POST request from above

<br><b>Bug</b>: The response back is a 200, and it should be a 404 to indicate that the key already exists. 
<hr>

DELETE request should delete a key when it exists in REDIS - Currently a successful message is returned indicated as if the key/value pair is deleted, but it is not actually deleted in REDIS.

Test to see this: `deleteKey`

To reproduce:
1) Ensure a key exists in REDIS - this can be done one of many ways...
- connect to redis-cli and find a key using the `keys *` command
- connect to redis-cli and confirm the existence of a key using `GET <KEY>` and ensuring the value is returned
- issuing a post command using this same API to create a key 
2) issue a DELETE request to delete that same key you just verified exists
```shell
curl --location --request DELETE 'https://qa-debugging-exercise.herokuapp.com/keys?name=<KEY>' \
--header 'token: QLNoqW87JWVjdTEn'
```
3) See the status come back as `200`
4) Go back to REDIS cli to see the state of that key. 
```GET <KEY>```

<br><b>Bug</b>: The `GET` command returns the value - which indicates the key is still present and was not deleted. The DELETE request should have deleted the key/value pair from REDIS

<hr>

Using HTML encoded characters (e.g. %20) on requests look as if they are successful by API response, but they are not actually affecting REDIS.

Test to see this: `postHTMLEncodedChars`

To reproduce: 

Note: This can be mimicked with any of the requests, I'll demo the POST (and that is what the test shows as well)

1) Make a POST request with the HTML encoded character of any character  (e..g %20)
```shell
curl --location --request POST 'http://qa-debugging-exercise.herokuapp.com/keys?name=%20&value=%20' \
--header 'token: QLNoqW87JWVjdTEn'
```
This request returns a 200 response. 
2) Query redis( via redis-cli) for that entry. 
```shell
GET %20
```

<br><b>Bug</b>: Even though the request was shown as successful, the insertion never occurred in REDIS. To verify this is actually an API issue as opposed to a REDIS issue, I attempted to insert that value directly into redis by using redis-cli and saw that it was inserted. 

<hr>

DELETE/POST/PUT requests should return a 4XX level error message (in theory, it is not actually specified) when the key/value parameters are not passed on the request. (Key/value parameters in totally, not to be confused with a null/empty redis key/value pairs)

To reproduce: -- This can be reproduced with any of the 3 methods, I will give examples for each. 

Test: `missingParameters`  <- This only covers the POST 

- Issue a DELETE request without the key parameter on the request.
```shell
curl --location --request DELETE 'https://qa-debugging-exercise.herokuapp.com/keys' \
--header 'token: QLNoqW87JWVjdTEn'
```
- Issue a POST request without the name parameter
```shell
curl --location --request POST 'http://qa-debugging-exercise.herokuapp.com/keys?value=bar' \
--header 'token: QLNoqW87JWVjdTEn'
```

Issue a POST request without either name or value 
```shell
curl --location --request POST 'http://qa-debugging-exercise.herokuapp.com/keys' \
--header 'token: QLNoqW87JWVjdTEn'
```

Issue a PUT request without name
```shell
curl --location --request PUT 'https://qa-debugging-exercise.herokuapp.com/keys?value=value4' \
--header 'token: QLNoqW87JWVjdTEn'
```

Issue a PUT request without name or value
```shell
curl --location --request PUT 'https://qa-debugging-exercise.herokuapp.com/keys' \
--header 'token: QLNoqW87JWVjdTEn'
```

<br><b>Bug</b>: a 500 response is returned (contents just below this line) - While the actual requirements aren't technically specified (what should happen when there is a missing parameter) the requirements only mention that a parameter is required --- the likely desired result is likely a 4XX error code with a friendlier response message.
```shell
<!DOCTYPE html>
<html>

<head>
	<title>We're sorry, but something went wrong (500)</title>
	<meta name="viewport" content="width=device-width,initial-scale=1">
	<style>
		.rails-default-error-page {
			background-color: #EFEFEF;
			color: #2E2F30;
			text-align: center;
			font-family: arial, sans-serif;
			margin: 0;
		}

		.rails-default-error-page div.dialog {
			width: 95%;
			max-width: 33em;
			margin: 4em auto 0;
		}

		.rails-default-error-page div.dialog>div {
			border: 1px solid #CCC;
			border-right-color: #999;
			border-left-color: #999;
			border-bottom-color: #BBB;
			border-top: #B00100 solid 4px;
			border-top-left-radius: 9px;
			border-top-right-radius: 9px;
			background-color: white;
			padding: 7px 12% 0;
			box-shadow: 0 3px 8px rgba(50, 50, 50, 0.17);
		}

		.rails-default-error-page h1 {
			font-size: 100%;
			color: #730E15;
			line-height: 1.5em;
		}

		.rails-default-error-page div.dialog>p {
			margin: 0 0 1em;
			padding: 1em;
			background-color: #F7F7F7;
			border: 1px solid #CCC;
			border-right-color: #999;
			border-left-color: #999;
			border-bottom-color: #999;
			border-bottom-left-radius: 4px;
			border-bottom-right-radius: 4px;
			border-top-color: #DADADA;
			color: #666;
			box-shadow: 0 3px 8px rgba(50, 50, 50, 0.17);
		}
	</style>
</head>

<body class="rails-default-error-page">
	<!-- This file lives in public/500.html -->
	<div class="dialog">
		<div>
			<h1>We're sorry, but something went wrong.</h1>
		</div>
		<p>If you are the application owner check the logs for more information.</p>
	</div>
</body>

</html>
```
<hr>

Making a GET (or any other HTTP method that is unsupported) request should return a 405 rather than a 404 and the response body should be JSON format rather than HTML 
Another way to phrase this or look at this is to make a request to a route that (according to documentation) does not exist should return a 404 with a JSON response body rather than HTML

Test: `unsupportedMethod`   <- only covers the GET case

To reproduce 
1) Make a GET request (explicitly unsupported)
```shell
curl --location --request GET 'http://qa-debugging-exercise.herokuapp.com/' \
--header 'token: QLNoqW87JWVjdTEn'
```

<br><b>Bug</b>: Response code is 404, when it should probably be a 405. Additionaly the body should be in JSON format with some error context rather than the HTML that is returned (pasted below)

```shell
curl --location --request POST 'http://qa-debugging-exercise.herokuapp.com/xyz' \
--header 'token: QLNoqW87JWVjdTEn'
```

```
<!DOCTYPE html>
<html>

<head>
	<title>The page you were looking for doesn't exist (404)</title>
	<meta name="viewport" content="width=device-width,initial-scale=1">
	<style>
		.rails-default-error-page {
			background-color: #EFEFEF;
			color: #2E2F30;
			text-align: center;
			font-family: arial, sans-serif;
			margin: 0;
		}

		.rails-default-error-page div.dialog {
			width: 95%;
			max-width: 33em;
			margin: 4em auto 0;
		}

		.rails-default-error-page div.dialog>div {
			border: 1px solid #CCC;
			border-right-color: #999;
			border-left-color: #999;
			border-bottom-color: #BBB;
			border-top: #B00100 solid 4px;
			border-top-left-radius: 9px;
			border-top-right-radius: 9px;
			background-color: white;
			padding: 7px 12% 0;
			box-shadow: 0 3px 8px rgba(50, 50, 50, 0.17);
		}

		.rails-default-error-page h1 {
			font-size: 100%;
			color: #730E15;
			line-height: 1.5em;
		}

		.rails-default-error-page div.dialog>p {
			margin: 0 0 1em;
			padding: 1em;
			background-color: #F7F7F7;
			border: 1px solid #CCC;
			border-right-color: #999;
			border-left-color: #999;
			border-bottom-color: #999;
			border-bottom-left-radius: 4px;
			border-bottom-right-radius: 4px;
			border-top-color: #DADADA;
			color: #666;
			box-shadow: 0 3px 8px rgba(50, 50, 50, 0.17);
		}
	</style>
</head>

<body class="rails-default-error-page">
	<!-- This file lives in public/404.html -->
	<div class="dialog">
		<div>
			<h1>The page you were looking for doesn't exist.</h1>
			<p>You may have mistyped the address or the page may have moved.</p>
		</div>
		<p>If you are the application owner check the logs for more information.</p>
	</div>
</body>

</html>
```

<hr>



<b>Possible Improvement to be made</b>

Test: `timing`

There seem some intermittent performance problems on the API side.
This can be observed just by making postman and/or curl requests to get a feel for it. I wrote a 
test with the `@Performance` tag which should by no means be a standard to how performance tests are done.
This test does a few things to get more data on this point
1) Make a POST request (which in theory should be calling the `set` command under the hood - that is a guess though)
2) Take the currentMs before and after that call
3) Make a set call (again, in theory this is the same call that should be being made under the hood)
4) Take currentMS before and after that call 

After all that is done, we should be able to measure the time the set call takes on its own and how long the api call takes and see the difference. This shouldn't be used in isolation or single snapshots but over time in a cleaner fashion to get an idea of what the API overhead is and to see if there is a problem that needs to be addressed. 

Currently, this test will fail intermittently - the threshold is set to the API request taking 3000ms or longer than the set.