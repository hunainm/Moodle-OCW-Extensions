# Scripts Setup

Before you start this process, make sure that the moodle is up and running with mysql.

* Install Python 2.7
* Install pip
* Use pip to install nltk, mysql-python
* Install [pyml](http://pyml.sourceforge.net/tutorial.html) packages
* In node_classifier.py change
```python
    db = MySQLdb.connect(db="fyp", passwd="12345678", user='root')
```
  to
```python
    db = MySQLdb.connect(db="YOUR_MOODLE_DB", passwd="YOUR_PASSWORD", user='YOUR_USERNAME')
``` 
* Run *node_classifier.py* with python to ensure there are no errors, it should produce a file *node_categories.txt* containing node categories.

