__author__ = 'zaintq'

import os
import sys
import io
import nltk
from nltk.corpus import stopwords
from PyML import *
from PyML.classifiers.multi import OneAgainstOne

path = "testing_data/test_query.data"

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

def normalize(tokens):
    total = sum(tokens.values())
    for key in tokens.keys():
        value = float(tokens[key]) / total
        tokens[key] = round(value, 7)
    return tokens

def export_testset(tokens, export_path):
    with io.open(export_path, "w", encoding='utf-8') as file:
        string_  = ""
        for feature in tokens.keys():
            string_ += (feature + ":" + str(tokens[feature]) + " ")
        file.write(string_)

def create_testset(query):

    file_text = query.lower()

    tokens = {}

    stemmer = nltk.stem.PorterStemmer()
    tokenizer = nltk.RegexpTokenizer(r'\w+')

    tokenized_text = tokenizer.tokenize(file_text)
    filtered_words = [w for w in tokenized_text if not w in stopwords.words('english')]


    for word in filtered_words:
        stemmed_word = stemmer.stem(word)
        if stemmed_word in tokens.keys():
            tokens[stemmed_word] += 1
        else:
            tokens[stemmed_word] = 1

    export_testset(normalize(tokens), path)

def classify_query(query):

    create_testset(query)
    classifier = get_classifier()
    predictions = classify(classifier, path)

    print(predictions)
    export_to_csv(predictions)

    return predictions

def export_to_csv(list):
    file = open("query_categories.txt", "wb")
    for item in list:
        file.write(item + ',')

if __name__ == "__main__":
    #query = sys.argv[1]
    query = "hockey is hockey and i love hockey"
    classify_query(query)