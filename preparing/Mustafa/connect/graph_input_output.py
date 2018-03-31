'''
Credit:
https://www.tutorialspoint.com/python/python_command_line_arguments.htm
https://stackoverflow.com/questions/43517959/given-a-tensor-flow-model-graph-how-to-find-the-input-node-and-output-node-name/43535500#43535500


'''


import tensorflow as tf
import sys, getopt

def finder(inputgraph, retrained):
    gf = tf.GraphDef()
    gf.ParseFromString(open(inputgraph,'rb').read())
    interested_nodes = [n.name + '=>' +  n.op for n in gf.node if n.op in ( 'Softmax','Mul')]
    print interested_nodes
    '''
    input_nodes = [[n.name + '=>' +  n.op for n in gf.node if n.op in ( 'Softmax','Placeholder')]]
    output_nodes = []
    if retrained == 1:
        output_nodes = [n.name + '=>' +  n.op for n in gf.node if n.op in ( 'Softmax','Mul')]
    else
        output_nodes = ['Mul=>Placeholder', 'final_result=>Softmax']
    '''



def main(argv):
   inputfile = ''
   retrained = 0
   try:
      opts, args = getopt.getopt(argv,"i:o:",["input=","retrained="])
   except getopt.GetoptError:
      print 'test.py -input <inputfile> -retrained [0,1]'
      sys.exit(2)
   for opt, arg in opts:
      if opt in ("--input"):
         inputfile = arg
      elif opt in ("--retrained"):
         if int(arg) >= 0 and int(arg) <3:
            retrained = arg
   print 'Input file is "', inputfile
   print 'retrained "', retrained
   finder(inputfile, retrained)

if __name__ == "__main__":
   main(sys.argv[1:])
