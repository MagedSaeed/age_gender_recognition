from sklearn.model_selection import train_test_split

file_names = ["imdb_age", "imdb_gender", "wiki_age", "wiki_gender"]
for file_name in file_names:
    with open("./txt_files/" + file_name + ".txt", "r") as f:
        content = f.readlines()
    # you may also want to remove whitespace characters like `\n` at the end of each line
    content = [x.strip() for x in content]
    train_set, val_set = train_test_split(content, test_size=0.1, random_state=2017)

    with open ("./txt_files/" + file_name + "_train.txt", "w") as f:
        for i in range(0, len(train_set)):
            f.write(str(train_set[i]) + '\n')
    with open ("./txt_files/" + file_name + "_val.txt", "w") as f:
        for i in range(0, len(val_set)):
            f.write(str(train_set[i]) + '\n')

    content, train_set, val_set = [] , [], []