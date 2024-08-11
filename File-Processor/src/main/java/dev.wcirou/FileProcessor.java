package dev.wcirou;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FileProcessor implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    //Creating Class Wide Utils Object to Access Methods
    private final Utils utils = new Utils();
    //Class Wide S3 Client
    private final S3Client s3 =  S3Client
            .builder()
            .region(Region.US_EAST_1)
            .build();
    //Class Wide SQS Client
    private final SqsClient sqs = SqsClient
            .builder()
            .region(Region.US_EAST_1)
            .build();
    @Override
    public APIGatewayProxyResponseEvent handleRequest (APIGatewayProxyRequestEvent input, Context context) {
        System.setProperty("java.io.tmpdir", "/tmp");
        try {
            //Getting the Objects as Array of Input Streams
        List<File> fileList = null;
        try {
            fileList = utils.returnFileList(s3, input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //Grading files
        Map<String,String> studentGrades = null;
        try {
            studentGrades = utils.returnGrades(fileList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //Sending the Files to SQS
            utils.sendMessageToQueue(sqs, sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName("QueueForGrades").build()).queueUrl() ,studentGrades);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("Successfully pushed grades to queue");

        }catch (Exception e){
            System.out.println("Error of type Exception: "+e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Failed to push grades to queue, Error Message: "+e.getMessage());
        }
    }

}
