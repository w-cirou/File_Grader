package dev.wcirou;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {
    //Returns the text from a PDF file passed to it
    public String readPdfContent(File pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }

    //Transforms Input Stream into a File
    public File returnFileFromInputStream(InputStream inputStream, String fileName) throws IOException {
        // Ensure the file is written to the /tmp directory
        String tempDir = System.getProperty("java.io.tmpdir");
        String filePath = tempDir + File.separator + fileName;

        File pdfFile = new File(filePath);
        try (FileOutputStream fileOutputStream = new FileOutputStream(pdfFile)) {
            IOUtils.copy(inputStream, fileOutputStream);
        }
        return pdfFile;
    }

    //Returns a List of Files from the S3 bucket specified in the API call
    public List<File> returnFileList(S3Client s3, APIGatewayProxyRequestEvent input) throws IOException {

        //Getting Bucket Name
        String bucketName = input.getPathParameters().get("bucketName");

        //Creating List for Files
        List<File> files = new ArrayList<>();

        // List objects in the bucket
        ListObjectsV2Request listObjectsReq = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        //Do while loop to continue looping until all items in the bucket have been got
        ListObjectsV2Response listObjectsRes;
        do {
            listObjectsRes = s3.listObjectsV2(listObjectsReq);

            for (S3Object object : listObjectsRes.contents()) {
                // Get the InputStream for each object and add it to the list to be returned
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(object.key())
                        .build();

                ResponseInputStream<GetObjectResponse> objectData = s3.getObject(getObjectRequest);

                //Adding the file to the list
                files.add(returnFileFromInputStream(objectData, object.key()));
            }

            // If there are more than maxKeys keys in the bucket, get a continuation token and create new request with that token
            String token = listObjectsRes.nextContinuationToken();
            listObjectsReq = listObjectsReq.toBuilder()
                    .continuationToken(token)
                    .build();
        } while (listObjectsRes.isTruncated());

        return files;
    }

    //Grades the files and return a String Map for the students grades with the student's name as the key and the grade as the value
    public Map<String, String> returnGrades(List<File> files) throws IOException {
        //Getting Answers from Answer Key File and creating a String Map with them in it
        Map <String, String> answerKey = new HashMap<>();
        String assignmentName = "";
        int idx = 0;
        for (File currentFile:files){
            if (currentFile.getPath().toLowerCase().contains("answerkey")) {
                String[] lines = readPdfContent(currentFile).split("\n");
                for (String line:lines){
                    if (line.contains(":")) { //Conditional Logic to check if the line contains a colon which signifies that line contains a question number and answer
                        answerKey.put(line.split(":")[0], line.split(":")[1]);
                    }
                }
                assignmentName = currentFile.getPath().split("_")[0].replace("/tmp","");
                //Removing Answer Key File
                files.remove(idx);
                break;
            }
            idx++;
        }
        //Grading files based on answer key and returning list of students grades
        Map<String, String> studentGrades = new HashMap<>();
        for (File currentFile:files){
            //Getting Student Name based on the Key that the File in S3 had
            String studentName = currentFile.getPath().split("_")[0].replace("/tmp","");
            //Grading the answer to each question which are one per line
            String[] lines = readPdfContent(currentFile).split("\n");
            int numCorrect = 0;
            for (String line:lines){
                if(line.contains(":")) { //Conditional Logic to check if the line contains a colon which signifies that line contains a question number and answer
                    if (answerKey.get(line.split(":")[0]).equals(line.split(":")[1])) {
                        numCorrect++;
                    }
                }
            }
            studentGrades.put(studentName, String.valueOf((float)100*((float) numCorrect /answerKey.size())));
        }
        //Adding the assignment name to the Map
        studentGrades.put("AssignmentName", assignmentName);
        return studentGrades;
    }

    //Pushes students grades to an SQS queue to be inputted into DynamoDB and returns string verifying if successful
    public String sendMessageToQueue(SqsClient sqsClient, String queueUrl, Map<String,String> studentGrades){
        //Creating message
        String msg = "";
        for (Map.Entry<String, String> entry : studentGrades.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            msg += key+"="+value+"\n";
        }

        // Create a SendMessageRequest with the message body
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(msg)
                .build();

        // Send the message to the queue
        SendMessageResponse response = sqsClient.sendMessage(sendMessageRequest);
        if (response.sdkHttpResponse().isSuccessful()) {
            return "Message sent successfully!";
        }else{
            return "Failed to send message";
        }
    }
    public String putItemInDynamoDB(DynamoDbClient ddb, String tableName, String StudentName, String AssignmentName, String Grade){
        // Create a map to represent the item
        Map<String, AttributeValue> itemValues = new HashMap<>();
        itemValues.put("StudentName", AttributeValue.builder().s(StudentName.replace("/","")).build());
        itemValues.put("AssignmentName", AttributeValue.builder().s(AssignmentName.replace("/","")).build());
        itemValues.put("Grade", AttributeValue.builder().n(Grade).build());

        // Create a PutItemRequest
        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(tableName)
                .item(itemValues)
                .build();

        // Put the item into the table
        PutItemResponse putItemResponse = ddb.putItem(putItemRequest);
        if (putItemResponse.sdkHttpResponse().isSuccessful()) {
            return "Item added successfully!";
        }else{
            return "Failed to add item";
        }
    }
    public List<Map<String, AttributeValue>> queryTable(DynamoDbClient ddb, APIGatewayProxyRequestEvent input) {
        //Getting partition key value from the request
        String partitionKeyValue = input.getPathParameters().get("assignmentName");
        // Create a map for the key condition expression
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":pk", AttributeValue.builder().s(partitionKeyValue).build());

        // Create a QueryRequest
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(System.getenv("TABLE_NAME"))
                .keyConditionExpression("AssignmentName = :pk")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        // Query the table and return items
        return ddb.query(queryRequest).items();
    }
}
