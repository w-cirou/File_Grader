package dev.wcirou;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetGradesForAssignment implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    //Class wide DynamoDB Client
    private final DynamoDbClient ddb = DynamoDbClient
            .builder()
            .region(Region.US_EAST_1)
            .build();
    //Creating Utils Object to access methods
    private final Utils utils = new Utils();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context){
        try {
            //Querying Table
            List<Map<String, AttributeValue>> queryResult = utils.queryTable(ddb, input);
            //Filling Map with student grades
            Map<String, String> studentGrades = new HashMap<>();
            for (Map<String, AttributeValue> item : queryResult) {
                studentGrades.put(item.get("StudentName").s(), item.get("Grade").n());
            }
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(studentGrades.toString());
        }catch(Exception e){
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Error of type Exception with message: "+e.getMessage());
        }
    }

}
