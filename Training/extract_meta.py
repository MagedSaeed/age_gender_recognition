import numpy as np
from scipy.io import loadmat
from datetime import datetime
import os



def calc_age(taken, dob):
    birth = datetime.fromordinal(max(int(dob) - 366, 1))

    # assume the photo was taken in the middle of the year
    if birth.month < 7:
        return taken - birth.year
    else:
        return taken - birth.year - 1


def get_meta(mat_path, db):
    meta = loadmat(mat_path)
    full_path = meta[db][0, 0]["full_path"][0]
    dob = meta[db][0, 0]["dob"][0]  # Matlab serial date number
    gender = meta[db][0, 0]["gender"][0]
    photo_taken = meta[db][0, 0]["photo_taken"][0]  # year
    face_score = meta[db][0, 0]["face_score"][0]
    second_face_score = meta[db][0, 0]["second_face_score"][0]
    age = [calc_age(photo_taken[i], dob[i]) for i in range(len(dob))]

    return full_path, dob, gender, photo_taken, face_score, second_face_score, age

imdb_age_path = "./txt_files/imdb_age.txt"
wiki_age_path = "./txt_files/wiki_age.txt"
imdb_gender_path = "./txt_files/imdb_gender.txt"
wiki_gender_path = "./txt_files/wiki_gender.txt"
directory = os.path.dirname(imdb_age_path)
print (directory)
if not os.path.exists(directory):
    print("Creating txt_files folder!")
    os.makedirs(directory)


full_path, dob, gender, photo_taken, face_score, second_face_score, age = get_meta("C:/Users/TOSHIBA/Desktop/172/ICS_411_Senior_Project/Dataset/IMDB_WIKI_CROPPED/imdb.mat", "imdb")
file = open (imdb_age_path, "w")
for i in range(0,full_path.shape[0]):
    if age[i] < 0 or age[i]>100:
        continue
    else:
        file.write(str(full_path[i][0]) + " " +str(age[i]) + "\n" )
file.close()

file = open (imdb_gender_path, "w")
for i in range(0,full_path.shape[0]):
    file.write(str(full_path[i][0]) + " " +str(gender[i]) + "\n" )
file.close()

full_path, dob, gender, photo_taken, face_score, second_face_score, age = get_meta("C:/Users/TOSHIBA/Desktop/172/ICS_411_Senior_Project/Dataset/IMDB_WIKI_CROPPED/wiki.mat", "wiki")
file = open (wiki_age_path, "w")
for i in range(0,full_path.shape[0]):
    if age[i] < 0 or age[i]>100:
        continue
    else:
        file.write(str(full_path[i][0]) + " " +str(age[i]) + "\n" )
file.close()

file = open (wiki_gender_path, "w")
for i in range(0,full_path.shape[0]):
    file.write(str(full_path[i][0]) + " " +str(gender[i]) + "\n" )
file.close()
