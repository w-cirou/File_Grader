package dev.wcirou;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;

public class InsertGradesToDynamoDB implements RequestHandler<SQSEvent,String> {
    //Class Wide DynamoDB Client
    private final DynamoDbClient ddb = DynamoDbClient
            .builder()
            .region(Region.US_EAST_1)
            .build();
    //Creating Utils Object to Access Methods
    private final Utils utils = new Utils();
    @Override
    public String handleRequest(SQSEvent input, Context context) {
        try {
            //Getting message from queue
            String[] messageLines = input.getRecords().get(0).getBody().split("\n");

            //Getting assignmentName
            String assignmentName = "";
            for (String line : messageLines) {
                if (line.split("=")[0].equals("AssignmentName")) {
                    assignmentName = line.split("=")[1];
                }
            }

            //Adding Items to table, expect for assignment name as its own item it is included as an attribute on each corresponding item
            for (String line : messageLines) {
                if (!line.split("=")[0].equals("AssignmentName")) {
                    utils.putItemInDynamoDB(ddb, System.getenv("TABLE_NAME"), line.split("=")[0], assignmentName, line.split("=")[1]);
                }
            }
            return "Successfully Inputted Grades to Table";

        }catch (Exception e){
            System.out.println("Error of type Exception: "+e.getMessage());
            return "Failed to Input Grades to Table";
        }
    }
}
