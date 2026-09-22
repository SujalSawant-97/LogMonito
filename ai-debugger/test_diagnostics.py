from diagnostics import diagnose_spring_error
import json

test_err = "org.springframework.dao.DataAccessResourceFailureException: com.mongodb.MongoSocketOpenException: Exception opening socket to localhost:27017"
diag = diagnose_spring_error(test_err, "order-service")
print("=== DIAGNOSIS TEST OUTPUT ===")
print(json.dumps(diag, indent=2))
