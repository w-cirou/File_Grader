package dev.wcirou;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public class GetAverageGrade implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    //Class wide DynamoDB Client
    private final DynamoDbClient ddb = DynamoDbClient
            .builder()
            .region(Region.US_EAST_1)
            .build();
    //Creating Utils Object to access methods
    private final Utils utils = new Utils();
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            //Getting items
            List<Map<String, AttributeValue>> items = utils.queryTable(ddb, input);
            double totalGrade = 0;
            int count = 0;
            for (Map<String, AttributeValue> item : items) {
                totalGrade += Integer.parseInt(item.get("Grade").n());
                count++;
            }
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("Assignment: "+input.getPathParameters().get("assignmentName")+"\nAverage Grade: "+ totalGrade/count);
        }catch (Exception e){
            System.out.println("Error of type Exception: "+e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error: "+e.getMessage());
        }

    }
}
