import pymongo

c = pymongo.MongoClient("mongodb://localhost:27017")
db = c["ai_monitoring_db"]
print("Collections:", db.list_collection_names())
for coll in db.list_collection_names():
    print(f"Collection: {coll} -> Count: {db[coll].count_documents({})}")
    first_doc = db[coll].find_one()
    if first_doc:
        print(f"  Sample: {first_doc}")
