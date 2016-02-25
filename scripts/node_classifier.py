__author__ = 'zaintq'

import io
import MySQLdb
from PyML import *
from PyML.classifiers.multi import OneAgainstOne

export_path = r"testing_data/test_doc_%s.data"

def get_classifier(train_path = 'train.data'):
    data = SparseDataSet(train_path)
    mc = OneAgainstOne (SVM())
    mc.train(data)
    return mc

def classify(classifier, test_path, train_path = 'train.data'):

    testData = SparseDataSet(test_path)
    r = classifier.test(testData)

    predicted_labels =  r.getPredictedLabels()

    return predicted_labels

def get_from_db():
    db = MySQLdb.connect(db="fyp", passwd="12345678", user='root')

    cursor = db.cursor()

    cursor.execute("SELECT * FROM son_terms")
    son_terms = cursor.fetchall()

    cursor.execute("SELECT doc_id, doc_size FROM son_documents")
    son_documents = cursor.fetchall()

    doc_size = {}
    doc_terms =  {}

    for row in son_documents:
        doc_size[int(row[0])] = int(row[1])

    for row in son_terms:
        doc_id = int(row[1])
        if doc_id not in doc_terms.keys():
            doc_terms[doc_id] = {row[0] : round(int(row[2]) / float(doc_size[doc_id]), 7)}
        else:
            doc_terms[doc_id][row[0]] = round(int(row[2]) / float(doc_size[doc_id]), 7)

    return doc_terms

def export_testset(tokens, export_path):
    with io.open(export_path, "wb") as file:
        string_  = r""
        for feature in tokens.keys():
            string_ += (" " + feature + ":" + str(tokens[feature]))
        file.write(string_)

def create_testset(docs):

    for doc in docs.items():
        export_testset(doc[1], export_path%doc[0])

def classify_node():

    docs = get_from_db()
    create_testset(docs)
    list = []
    classifier = get_classifier()
    hash_table = {}
    for doc in docs.keys():
        category = classify(classifier, export_path%doc)[0]
        if category not in hash_table.keys():
            hash_table[category] = ""
            list.append(category)

    print(list)
    export_to_csv(list)
    return list

def export_to_csv(list):
    file = open("node_categories.txt", "wb")
    for item in list:
        file.write(item + ',')

if __name__ == "__main__":
    classify_node()
