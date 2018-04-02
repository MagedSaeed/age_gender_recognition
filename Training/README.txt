Download the data set from the following links:
IMDB (faces only) -- 7 GB
https://data.vision.ee.ethz.ch/cvl/rrothe/imdb-wiki/static/imdb_crop.tar
WIKI (faces only) -- 1 GB
https://data.vision.ee.ethz.ch/cvl/rrothe/imdb-wiki/static/wiki_crop.tar

Download the metadata from the following links:
imdb.mat
https://data.vision.ee.ethz.ch/cvl/rrothe/imdb-wiki/static/imdb_meta.tar
wiki.mat
https://data.vision.ee.ethz.ch/cvl/rrothe/imdb-wiki/static/wiki.tar.gz

Use extract_meta.py to create the following four files:
imdb_age.txt
imdb_gender.txt
wiki_age.txt
wiki_gender.txt

They contain the training examples with thier labels in the following form:
folder/image label
folder/image label
...

Use split.py to split each of the above four files into two files, one holding the training set and one holding the validation set.


The following changes should be done to the files mentioned below in rude-carnie repo:
*preproc.py
The following flags
fold_dir to point to where training and validation splits files 
data_dir to point to where the images are.
output_dir to point to the desired output dir for the TFRecords

*train.py
The following flags:
train_dir to point to the folder having the TFRecord created using preproc.py


Use preproc.py to create TFRecords 


and now you are ready for training using train.py




