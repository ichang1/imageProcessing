import numpy as np
from collections import deque
class Quadtree:
    def __init__(self, 
                 colors: np.array):
        #rgb for original image
        self.colors = colors
        #number of leaf nodes
        self.numLeaves = 0
        #width of image in terms of pixels
        self.width = colors.shape[1]
        #height of image in terms of pixels
        self.height = colors.shape[0]
        #size of image in terms of pixels
        self.size = self.width*self.height
        self.threshold = 80
        #original image with no pixel compression
        self.root = Node(self.meanColor(0,0,self.width,self.height),
                         0, 
                         0,
                         self.width,
                         self.height,
                         0)
        #get child nodes from compression original image
        self._insert(self.root)
        #to hold rgb values for modified image 
        self.modified = np.empty((self.height,self.width),dtype=RGB)

    '''
                   mid_w      w - mid_w  
                -------------------------
                |          |            |
        mid_h   |          |            |
                |          |            |
                |----------|------------|
                |          |            |
    h - mid_h   |          |            | 
                |          |            |
                |          |            |
                -------------------------
    '''
    #insert children until threshold is broken
    def _insert(self, root):
        #whether a node should still be split based on threshold
        def reachThreshold(node):
            return False if self.avgSqError(node) > self.threshold else True

        if not root.height == 1 and not root.width == 1 and not reachThreshold(root):
            root_w = root.width
            root_h = root.height
            root_mid_w = root_w // 2
            root_mid_h = root_h // 2
            nEast_avgColor = self.meanColor(root.startRow,
                                            root.startCol + root_mid_w,
                                            root_w - root_mid_w,
                                            root_mid_h)
            root.nEast = Node(nEast_avgColor,
                              root.startRow,
                              root.startCol + root_mid_w,
                              root_w - root_mid_w,
                              root_mid_h,
                              root.level + 1)
            nWest_avgColor = self.meanColor(root.startRow,
                                            root.startCol,
                                            root_mid_w,
                                            root_mid_h)
            root.nWest = Node(nWest_avgColor,
                              root.startRow,
                              root.startCol,
                              root_mid_w,
                              root_mid_h,
                              root.level + 1)
            sEast_avgColor = self.meanColor(root.startRow + root_mid_h,
                                            root.startCol + root_mid_w,
                                            root_w - root_mid_w,
                                            root_h - root_mid_h)
            root.sEast = Node(sEast_avgColor,
                              root.startRow + root_mid_h,
                              root.startCol + root_mid_w,
                              root_w - root_mid_w,
                              root_h - root_mid_h,
                              root.level + 1)
            sWest_avgColor = self.meanColor(root.startRow,
                                            root.startCol + root_mid_h,
                                            root_mid_w,
                                            root_h - root_mid_h)
            root.sWest = Node(sWest_avgColor,
                              root.startRow,
                              root.startCol + root_mid_h,
                              root_mid_w,
                              root_h - root_mid_h,
                              root.level + 1)
            self._insert(root.nEast)
            self._insert(root.nWest)
            self._insert(root.sEast)
            self._insert(root.sWest)
        else:
            self.numLeaves += 1
    
        #mean rgb of all pixels in a region
    def meanColor(self, startRow, startCol, width, height):
        avgColor = RGB((0,0,0))
        for h in range(startRow, startRow + height):
            for w in range(startCol, startCol + width):
                avgColor += self.colors[h][w]
        size = height * width
        avgColor /= size
        return avgColor
    
    #avg square error between each pixel and the node's avg color
    def avgSqError(self, node):
        err = 0
        for h in range(node.startRow, node.startRow + node.height):
            for w in range(node.startCol, node.startCol + node.width):
                err += self.colors[h][w].sqError(node.avgColor)
        size = node.height*node.width
        return err/size

    #get leaf nodes of quadtree
    def _getLeaves(self):
        leaves = np.empty(self.numLeaves,dtype=Node)
        # in place to get leaves
        def dfs(leaves):
            i = 0 
            stack = deque()
            stack.append(self.root)
            while len(stack) != 0:
                n = stack.pop()
                if (n.nEast == None and 
                    n.nWest == None and 
                    n.sEast == None and 
                    n.sWest == None):
                    leaves[i] = n
                    i += 1
                else:
                    if n.nEast != None:
                        stack.append(n.nEast)
                    if n.nWest != None:
                        stack.append(n.nWest)
                    if n.sEast != None:
                        stack.append(n.sEast)
                    if n.sWest != None:
                        stack.append(n.sWest)
        dfs(leaves)
        return leaves

    def outline(self):
        #outline border that a node covers in original image and 
        # fills inside with the average color
        def outlineSquare(leaf: Node):
            minRow = leaf.startRow
            maxRow = minRow + leaf.height
            minCol = leaf.startCol
            maxCol = minCol + leaf.width
            for r in range(minRow, maxRow):
                for c in range(minCol, maxCol):
                    if r == minRow or r == maxRow or c == minCol or c == maxCol:
                        self.modified[r][c] = RGB((0,0,0))
                    else:
                        self.modified[r][c] = leaf.avgColor
        leaves = self._getLeaves()
        for leaf in leaves:
            outlineSquare(leaf)

class Node:
        def __init__(self,
                     avgColor,
                     startRow: int,
                     startCol: int,
                     width: int,
                     height: int,
                     level: int):
            self.avgColor = avgColor
            self.startRow = startRow
            self.startCol = startCol
            self.width = width
            self.height = height
            self.level = level
            self.nEast = None
            self.nWest = None
            self.sEast = None
            self.sWest = None

        def isOnePixel(self):
            return self.height == 1 and self.width == 1

class RGB:
    def __init__(self,rgb: tuple):
        self.r = rgb[0]
        self.g = rgb[1]
        self.b = rgb[2]

    def __add__(self, other):
        self.r += other.r
        self.g += other.g
        self.b += other.b
        return self

    def __sub__(self, other):
        self.r -= other.r
        self.g -= other.g
        self.b -= other.b
        return self
    
    def __rmul__(self, n):
        self.r *= n
        self.g *= n
        self.b *= n
        return self

    def __mul__(self, n):
        return self.rmul(n)

    def __floordiv__(self,n):
        self.r //= n
        self.g //= n
        self.b //= n
        return self

    def __truediv__(self,n):
        self.r /= n
        self.g /= n
        self.b /= n
        return self
    
    def getRGB(self):
        return (self.r,self.g,self.b)
    
    def sqError(self, other):
        diff = self - other
        return diff.r**2 + diff.g*2 + diff.b**2

    def __str__(self):
        return f'({self.r},{self.g},{self.b})'

    