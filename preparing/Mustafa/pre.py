#!python2
# This file is for preprocessing face images
# Author: Mustafa Al-Turki Feb, 2018

import pyvision as pv
import cv2
import matplotlib.pyplot as plt
import pyvision.face.CascadeDetector as cd
import pyvision.face.FilterEyeLocator as ed
import caffe
import lmdb # you can install it using pip install lmdb
import numpy as np
from caffe.proto import caffe_pb2






def preprocess (im):
	nim = normalize(im)
	return detect_face(nim)

# This function detect the face in image and return the cropped face if found, and the entire image otherwise.	
def detect_face(im):
	face_detector = cd.CascadeDetector() 
	faces = face_detector(im)
	# if faces list is empty, this mean no face can be detected
	# so I will change the orientation many times with different angles untill a face is detected.
	# if still no face can be found the entire image is returned.
	trial=1
	fivedegree= 0.0872665
	faces = face_detector(im)
	rim = None
	longestside=max(im.height,im.width)
	
	while not faces:
		if trial == 1: theta = 3.14159
		elif trial == 2: theta = 1.5708
		elif trial == 3: theta = -1.5708
		elif trial >= 4 and trial <= 28: theta = -1.0472 + ((trial-4)*fivedegree)
		else: return im 
		affine = pv.AffineRotate(theta=theta, new_size=(longestside,longestside), center= pv.Point(longestside/2,longestside/2),interpolate=2) 
		rim = affine.transformImage(im)
		faces = face_detector(rim)
		trial +=1
		
	if rim == None: rim = im #in case of no rotation was needed
	for face in faces:
		affine = pv.AffineFromRect(rect=face, new_size=(256,256), interpolate=2) 
		cim = affine.transformImage(rim,use_orig=False)
	return cim #this might be extended to work with images that has multiple faces 
	
	
def detect_eye(faceim):
	leye,reye = None
	face_detect = cd.CascadeDetector()
	eye_detect = ed.FilterEyeLocator()
	faces = face_detect(faceim)
	eyes = eye_detect(faceim,faces)
	for face,leye,reye in eyes:
		faceim.annotatePolygon(face.asPolygon(), width=4)
		faceim.annotatePoints([leye,reye])

	faceim.show(delay=0)
	cv2.destroyAllWindows()
	return (leye,reye)
	
	
	
# this function will align faces over each other, that is left eyes of all faces should be on top of each other if stacked, and all other face landmarks
# Question: is it that we align all images with the first image whatever image it was?
'''
def align(face):
	(leye,reye) = detect_eye(face)
	if leye is None or reye is None: return face
	
	return face
'''
	
def normalize(im):
	nim = pv.other.normalize.selfQuotientImage(im,sigma=5.0)
	return nim

 



if __name__ == '__main__':
	# Uncomment this code section and comment the code below it to use the preprocessing pipeline for one image
	
	# reading the image
	#im = cv2.imread('pyvision_notebooks/img/faceU.png', cv2.IMREAD_GRAYSCALE)
	im = pv.Image('pyvision_notebooks/img/me.jpg').asBW()
	#thumbnail resizes the image while preserving the aspect ration....resize alone will make the face undetectable sometimes	
	sim = im.thumbnail((512,512))
	fim = preprocess(sim)
	#saving the image may need to be moved to another place
	fim.show(delay=0)
	fim = fim.thumbnail((512,512))
	print fim.size
	cv2.imwrite('pyvision_notebooks/img/croped.jpg',fim.asOpenCV2())
	fim.show(delay=0)
	cv2.destroyAllWindows()
	'''
	
	
	# The code below will read a LMDB database and feed one image at a time to the preprocessing pipeline.
	# Credit: The code below is originally created by Wei Yang in 2015-08-19 and the following are the resourses the original author used
	# Source
	#   Read LevelDB/LMDB
	#   ==================
	#       http://research.beenfrog.com/code/2015/03/28/read-leveldb-lmdb-for-caffe-with-python.html
	#   Plot image
	#   ==================
	#       http://www.pyimagesearch.com/2014/11/03/display-matplotlib-rgb-image/
	#   Creating LMDB in python
	#   ==================
	#       http://deepdish.io/2015/04/28/creating-lmdb-in-python/
	# Modified by: Mustafa Al-Turki 2018
	
	lmdb_file = "C:/Users/TOSHIBA/Desktop/172/ICS_411_Senior_Project/lmdb/age_test_lmdb"
	lmdb_env = lmdb.open(lmdb_file)
	lmdb_txn = lmdb_env.begin()
	lmdb_cursor = lmdb_txn.cursor()
	datum = caffe_pb2.Datum()
	count =0
	for key, value in lmdb_cursor:
		datum.ParseFromString(value)
		label = datum.label
		data = caffe.io.datum_to_array(datum)
		image = data.astype(np.uint8)
		image = np.transpose(image, (2, 1, 0)) # original (dim, col, row)
		#print "label ", label
		count = count + 1
		try:
			im = pv.Image(image)
			#thumbnail resizes the image while preserving the aspect ration....resize alone will make the face undetectable sometimes	
			sim = im.thumbnail((256,256))
			fim = preprocess(sim)
			#saving the image may need to be moved to another place 
			#cv2.imwrite('pyvision_notebooks/img/croped.jpg',fim.asOpenCV2())
			fim.show(delay=0)
			cv2.destroyAllWindows()
			print count
		except:
			print "skipped"
			'''
			