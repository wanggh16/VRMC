import numpy
from random import random
from math import exp

N=300
f=open('scene_data.txt','w')

A=numpy.array([[random() for i in range(N)] for j in range(N)])
AL=numpy.abs(numpy.fft.ifft2(numpy.fft.fft2(A)*[[exp(-0.003*(i*i+j*j))+exp(-0.003*((N-i)*(N-i)+j*j))+exp(-0.003*(i*i+(N-j)*(N-j)))+exp(-0.003*((N-i)*(N-i)+(N-j)*(N-j))) for i in range(N)] for j in range(N)]))
AL=(AL-numpy.min(AL))/(numpy.max(AL)-numpy.min(AL))

D=numpy.array([[random() for i in range(N)] for j in range(N)])
DL=numpy.abs(numpy.fft.ifft2(numpy.fft.fft2(D)*[[exp(-0.003*(i*i+j*j))+exp(-0.003*((N-i)*(N-i)+j*j))+exp(-0.003*(i*i+(N-j)*(N-j)))+exp(-0.003*((N-i)*(N-i)+(N-j)*(N-j))) for i in range(N)] for j in range(N)]))
DL=(DL-numpy.min(DL))/(numpy.max(DL)-numpy.min(DL))

for i in range(len(AL)):
	for j in range(len(AL[i])):
		print('%.4f %.4f'%(AL[i][j],DL[i][j]),end='\t',file=f)
	print('',file=f)
print('',file=f)
