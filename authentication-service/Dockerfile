FROM public.ecr.aws/lambda/java:21
COPY target/quickbite-lambda-1.0-SNAPSHOT.jar ${LAMBDA_TASK_ROOT}/lib/
CMD ["com.example.lambda.CreateUserHandler::handleRequest"]
