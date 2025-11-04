## Tool calling also known as function calling is a common patter in AI appliction allowing a model to interact with a set of APIs, or tools, or methods augmenting(increasing) its capabilities.

## Usage of tools
1. Information Retrieval from external sources, such as a database, a web service, a file system, or a web search engine.
For example, a tool can be used to retrieve the current weather for a given location, to retrieve the latest news articles, or to query a database for a specific record.

2. Taking action:- Tools are used to actions like sending an email, creating a new record in a database, submitting a form, or triggering a workflow
Eg:- For example, a tool can be used to book a flight for a customer interacting with a chatbot, to fill out a form on a web page, or to implement a Java class based on an automated test (TDD) in a code generation scenario.

The model can only request a tools call and provide input arguments, whereas application is responsible for executing tool call from input args and return the result. Model never gets access to any of APIs provided as tools for security consideration.

## Information Retrieval Tool Calling implementation - Retrieve users time zone.

1. Implement DateTimeTools class
- DateTimeTool class implement a tool to get current date and time in user time zone. 
- tool is not taking any argument
- LocalContextHolder from spring provide user's time zone. 
- Tool is defined as a method annotated @Tool.
- To help model understanding when to call this tool, we'll provide a detailed descriotion of what tools does.

```
import java.time.LocalDateTime;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

class DateTimeTools {

    @Tool(description = "Get the current date and time in the user's timezone")
    String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

}
```
2. Make tool available to the model using "ChatClient" to interact with model.
- Provide tool to the model by passing an instance of DtaeTimeTools via tools() method.
- when model needs to know current date and time -> req tool to be called.
- Cahtclient call the tool and return result to model.

```
ChatModel chatModel = ...

String response = ChatClient.create(chatModel)
        .prompt("What day is tomorrow?")
        .tools(new DateTimeTools())
        .call()
        .content();

System.out.println(response);
```

## Taking Actions- set an alaram for specified time.
- An AI model can be used to generate a plan for accomplisihing certain golas. Howeveer, the model dont have ability to eecute plan. That's where tools comes in to execute plan model generates.
- Adding a new Tool setAlarm to the same DateTimeTools class. This is taking single param, time in ISO-8601 fromat.
- It consoles alarm set for given time.
- Provide a description to the tool to make model understand when and how to use tool.
- The @ToolParam annotation can be used to provide additional information about the input parameters, such as a description or whether the parameter is required or optional. By default, all input parameters are considered required.

```
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

class DateTimeTools {

    @Tool(description = "Get the current date and time in the user's timezone")
    String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

    @Tool(description = "Set a user alarm for the given time, provided in ISO-8601 format")
    void setAlarm(@ToolParam(description = "Time in ISO-8601 format") String time) {
        LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
        System.out.println("Alarm set for " + alarmTime);
    }

}
```

- We'll use chatClient to interact with model.
- Providing tools by passing DateTimeTools insteance via tools() method.
- To set a alarm for 10min from now on, it should know current time.
- So, it call getCurrentDateTiem first following the setAlarm.

```
ChatModel chatModel = ...

String response = ChatClient.create(chatModel)
        .prompt("Can you set an alarm 10 minutes from now?")
        .tools(new DateTimeTools())
        .call()
        .content();

System.out.println(response);
```

## Functions as Tools
- The follwing types are not supported as params or return types for methods used as tools.
- Optional , async types (Completeable Future, Future), Reactive types ( eg:- Flow, Mono, Flux) and functional types(eg:- Function, Supplier, Consumer)
- To support these types SPring AI provides built-in support for specifying tools suing functions i.e either FunctionToolCallBack implementation or dynamically as @Bean -> resolved at runtime.

  ##PRogrammatic specification (FunctionToolCallback)
  - we can turn a functional type (Function, Supplier, Consumer or BiFunction) into  a tool by building FunctionalToolCallback programmatically.
 
  -
    ```
    public class WeatherService implements Function<WeatherRequest, WeatherResponse> {
    public WeatherResponse apply(WeatherRequest request) {
        return new WeatherResponse(30.0, Unit.C);
    }
}

public enum Unit { C, F }
public record WeatherRequest(String location, Unit unit) {}
public record WeatherResponse(double temp, Unit unit) {}
    ```

- The FunctionToolCallback.Builder allows to build a FunctionToolCallback instance and provide key info about the tool.
- name: Name of the tool. AI model uses this name to identify the tool when calling it. So, it's not allowed to have tools with same name in the same context.
- toolFunction: The functional object that represents the tool method (Function, Supplier, Consumer or BiFunction)
- description:- Description for the tool, which can be used by the model to understand when and how to call the model.
- inputType: The type of the function input required.
- inputSchema - JSON schema for the input parameters of teh tool. If not provideed the schema will be generated automatically based on the input type.
- toolMetadata - The toolmetada instance define additional settings such as whether the result should be returned directly to the client and result converter to use. We can build ToolMetada using ToolMetadata.Builder.

```
ToolCallBack toolCallBack = FunctionToolCallBack
.builder("currentWeather", new WeatherService())
.description("Get the weather in location")
.inputType(WeatherRequest.class)
.build();
```
- Now we have to pass the FunctionToolCallBack instance to the toolCallbacks() method of chatClient.
```
ChatClient.create(chatModel)
.prompt("What's the weather in cincinnati")
.toolCallbacks(toolCallback)
.call()
.content();
```

## Adding default tools to chatModel
```
ToolCallback toolCallback = FunctionToolCallback.builder()...

ChatModel chatModel = OllamaChatModel.builder()
.ollamaApi(OllamaApi.builder().build())
.defaultOptions(
   ToolCallingChatOptions()
   .builder()
   .toolCallbacks(toolCallBack)
   .build())
.build();
```

- Instead of specifying tools programatically, we can define tools as Spring beans and let SpringAi dynamically resolve them using ToolCallbackResolver interface via SpringBootToolCallBackResolver implementation. This option allows us to use any Function,Supplier, Consumer or BiFunction bean as a tool. The bean name is used as a tool name and @Description annotaton from spring framework can be used to provide description fro the tool.

```
@Configuration(proxyBeanMethods = false)
class WeatherTools {
  WeatherService weatherService = new WeatherService();

  @Bean
  @Description("Get the weather in location")
  Function<WeatherRequest,WeatherResponse> currentWeather(){
    return weatherService;
  }
}
```

```
record WeatherRequest(@ToolParam(description = "The name of a city or a country") String location, Unit unit) {}
```

```
ChatClient.create(chatModel)
    .prompt("What's the weather like in Copenhagen?")
    .toolNames("currentWeather")
    .call()
    .content();
```

- For dynamic specification approach, you can pass the tool name to the toolNames() method of the ToolCallingChatOptions you use to call the ChatModel.
```
ChatModel chatModel = ...
ChatOptions chatOptions = ToolCallingChatOptions.builder()
    .toolNames("currentWeather")
    .build();
Prompt prompt = new Prompt("What's the weather like in Copenhagen?", chatOptions);
chatModel.call(prompt);
```
  


