import argparse
import os.path
import struct
import numpy as np
from quadtree import Quadtree, RGB
from PIL import Image

def is_valid_read_file(parser, arg):
    if not os.path.exists(arg):
        parser.error("The file %s does not exist!" % arg)
    else:
        return arg

parser = argparse.ArgumentParser()

parser.add_argument('image',
                    help='image file',
                    type=lambda x: is_valid_read_file(parser, x))
parser.add_argument('operation',
                    help='operation to do on image',
                    type=str)

args = parser.parse_args()
img_raw = Image.open(args.image)
img_rgb = img_raw.convert('RGB')
width, height = img_rgb.size
shape = (height, width)
colors = np.empty(shape,dtype=RGB)
pixels = img_rgb.load()
for h in range(height):
    for w in range(width):
        colors[h][w] = RGB(pixels[w,h])
qtree = Quadtree(colors)
modified = qtree.outline()




