package dev.wcirou;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;


import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;

//public class FileProcessorTest {
//    @Mock
//    APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
//    @Mock
//    Context context;
//    @Mock
//    LambdaLogger logger;
//    @InjectMocks
//    FileProcessor handler;
//
//
//    @BeforeEach
//    public void initialize_context_logger_and_handler() {
//        context = mock(Context.class);
//        logger = mock(LambdaLogger.class);
//        handler = new FileProcessor();
//    }
//    @Test
//    public void fileProcessorTest() {
//        Map<String,String> pathParameters =  new HashMap<>();
//        pathParameters.put("bucketName", "wcirou-files-to-be-graded");
//        input.setPathParameters(pathParameters);
//        APIGatewayProxyResponseEvent response = handler.handleRequest(input, context);
//        System.out.println(response.getBody());
//    }
//}
