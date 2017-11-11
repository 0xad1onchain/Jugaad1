
# coding: utf-8

# In[13]:


from collections import Counter
import tensorflow as tf
import numpy as np
import pandas as pd

data = pd.read_csv('https://venturesity-uploads.s3.amazonaws.com/dump/C2vvfJ/KfcV7m/TRAIN_SMS.csv', encoding='latin-1')
data.head(5)


data = data.rename(columns={"Message" : "text", "Label":"label"})


np.savetxt(r'messages.txt', data['text'].values, fmt='%s')
np.savetxt(r'labels.txt', data['label'].values, fmt='%s')

with open('messages.txt', encoding="ISO-8859-1") as f:
    messages = f.read()
with open('labels.txt',encoding="ISO-8859-1") as f:
    labels = f.read()
    
from string import punctuation
all_text = ''.join([c for c in messages if c not in punctuation])
messages = all_text.split('\n')

all_text = ' '.join(messages)
words = all_text.split()



split_words = Counter(words)
sorted_split_words = sorted(split_words, key=split_words.get, reverse=True)
vocab_to_int = {c : i for i, c in enumerate(sorted_split_words,1)}

# Convert the reviews to integers, same shape as reviews list, but with integers
messages_ints = []
for message in messages:
    messages_ints.append([vocab_to_int[i] for i in message.split()])
    


# In[14]:


labels = labels.split("\n")
labels = np.array([0 if label == "ham" else (1 if label == 'info' else 2 ) for label in labels])
print (labels)


from collections import Counter

message_lens = Counter([len(x) for x in messages_ints])
print("Zero-length messages: {}".format(message_lens[0]))
print("Maximum message length: {}".format(max(message_lens)))


messages_ints = [message for message in messages_ints if (len(message)>0)]

seq_len = 200
num_messages = len(messages)
features = np.zeros([num_messages, seq_len], dtype=int)
for i, row in enumerate(messages_ints):
    features[i, -len(row):] = np.array(row)[:seq_len]
    
    
features[0]


split_frac1 = 0.8

idx1 = int(len(features) * split_frac1)
train_x, val_x = features[:idx1], features[idx1:]
train_y, val_y = labels[:idx1], labels[idx1:]

split_frac2 = 0.5
idx2 = int(len(val_x) * split_frac2)
val_x, test_x = val_x[:idx2], val_x[idx2:]
val_y, test_y = val_y[:idx2], val_y[idx2:]

print("\t\t\tFeature Shapes:")
print("Train set: \t\t{}".format(train_x.shape), 
      "\nValidation set: \t{}".format(val_x.shape),
      "\nTest set: \t\t{}".format(test_x.shape))

print("\t\t\Label Shapes:")
print("Train set: \t\t{}".format(train_y.shape), 
      "\nValidation set: \t{}".format(val_y.shape),
      "\nTest set: \t\t{}".format(test_y.shape))


# In[15]:



from sklearn.linear_model import LogisticRegression
from sklearn.naive_bayes import MultinomialNB
from sklearn.metrics import accuracy_score

clf = LogisticRegression()
clf.fit(train_x,train_y)
p = clf.predict(val_x)
print (accuracy_score(val_y,p))

lstm_size = 256
lstm_layers = 1
batch_size = 256
learning_rate = 0.003


n_words = len(sorted_split_words)

# Create the graph object
graph = tf.Graph()
# Add nodes to the graph
with graph.as_default():
    inputs_ = tf.placeholder(tf.int32, [None,None], name = "inputs")
    labels_ = tf.placeholder(tf.int32, [None,None], name = "labels")
    keep_prob = tf.placeholder(tf.float32, name = "keep_prob")
    
    
# Size of the embedding vectors (number of units in the embedding layer)
embed_size = 300 

with graph.as_default():
    embedding = tf.Variable(tf.random_uniform((n_words, embed_size), -1, 1))
    embed = tf.nn.embedding_lookup(embedding, inputs_)
    
    
with graph.as_default():
    # Your basic LSTM cell
    lstm = tf.contrib.rnn.BasicLSTMCell(lstm_size)
    
    # Add dropout to the cell
    drop = tf.contrib.rnn.DropoutWrapper(lstm, output_keep_prob=keep_prob)
    
    # Stack up multiple LSTM layers, for deep learning
    cell = tf.contrib.rnn.MultiRNNCell([drop] * lstm_layers)
    
    # Getting an initial state of all zeros
    initial_state = cell.zero_state(batch_size, tf.float32)
    
    
with graph.as_default():
    outputs, final_state = tf.nn.dynamic_rnn(cell, embed, initial_state=initial_state)
    
    
with graph.as_default():
    predictions = tf.contrib.layers.fully_connected(outputs[:, -1], 1, activation_fn=tf.sigmoid)
    cost = tf.losses.mean_squared_error(labels_, predictions)
    
    optimizer = tf.train.AdamOptimizer(learning_rate).minimize(cost)
    
    
with graph.as_default():
    correct_pred = tf.equal(tf.cast(tf.round(predictions), tf.int32), labels_)
    accuracy = tf.reduce_mean(tf.cast(correct_pred, tf.float32))
    
    
def get_batches(x, y, batch_size=100):
    
    n_batches = len(x)//batch_size
    x, y = x[:n_batches*batch_size], y[:n_batches*batch_size]
    for ii in range(0, len(x), batch_size):
        yield x[ii:ii+batch_size], y[ii:ii+batch_size]
        
        
        
        
        


# In[16]:


epochs = 5

with graph.as_default():
    saver = tf.train.Saver()

with tf.Session(graph=graph) as sess:
    sess.run(tf.global_variables_initializer())
    iteration = 1
    for e in range(epochs):
        state = sess.run(initial_state)
        
        for ii, (x, y) in enumerate(get_batches(train_x, train_y, batch_size), 1):
            feed = {inputs_: x,
                    labels_: y[:, None],
                    keep_prob: 0.5,
                    initial_state: state}
            loss, state, _ = sess.run([cost, final_state, optimizer], feed_dict=feed)
            
            if iteration%5==0:
                print("Epoch: {}/{}".format(e, epochs),
                      "Iteration: {}".format(iteration),
                      "Train loss: {:.3f}".format(loss))

            if iteration%25==0:
                val_acc = []
                val_state = sess.run(cell.zero_state(batch_size, tf.float32))
                for x, y in get_batches(val_x, val_y, batch_size):
                    feed = {inputs_: x,
                            labels_: y[:, None],
                            keep_prob: 1,
                            initial_state: val_state}
                    batch_acc, val_state = sess.run([accuracy, final_state], feed_dict=feed)
                    val_acc.append(batch_acc)
                print("Val acc: {:.3f}".format(np.mean(val_acc)))
            iteration +=1
    saver.save(sess, "checkpoints/sentiment.ckpt")


# In[ ]:


test_acc = []
with tf.Session(graph=graph) as sess:
    saver.restore(sess, tf.train.latest_checkpoint('checkpoints'))
    test_state = sess.run(cell.zero_state(batch_size, tf.float32))
    for ii, (x, y) in enumerate(get_batches(test_x, test_y, batch_size), 1):
        feed = {inputs_: x,
                labels_: y[:, None],
                keep_prob: 1,
                initial_state: test_state}
        batch_acc, test_state = sess.run([accuracy, final_state], feed_dict=feed)
        test_acc.append(batch_acc)
    print("Test accuracy: {:.3f}".format(np.mean(test_acc)))
    
    
text = 'Hello You just won yourself a free tour to Bahamas!! Call Now to recieve it.'
text = ''.join([c for c in text if c not in punctuation])
integer = ([vocab_to_int[i] for i in text.split(" ")])



i = []
integer = np.array(integer)
i.append(integer)


i = np.array(i)
i.shape


with tf.Session(graph=graph) as sess:
    ckpt = tf.train.get_checkpoint_state('./checkpoints')
    state = sess.run(initial_state)
    saver.restore(sess, ckpt.model_checkpoint_path)
    feed_dict = {inputs_: i, 
                 initial_state: state,
                 keep_prob: 0.5}
    predictions = sess.run(predictions, feed_dict = feed_dict)

print("Test accuracy: {:.3f}".format(predictions))

