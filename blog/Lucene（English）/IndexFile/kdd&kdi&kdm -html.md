# [kdd&kdi&kdm](https://www.amazingkoala.com.cn/Lucene_Document/IndexFile/)（Lucene 8.6.0）

&emsp;&emsp;The data of Point  is encoded in a block KD-tree structure described with three index files：

-	A .kdm file that records metadata about the fields
-	A .kdi file that stores inner nodes of the tree
-	A .kdd file that stores leaf nodes of the tree

## .kdd

&emsp;&emsp;The data structure of .kdd file as below: 

Fig.1：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/1.png">

&emsp;&emsp;**LeafNodeData** describes the informations of leaf nodes, You can see there is only leaf node data in .kdd file and the default maximum number of point in each **LeafNodeData**  is 512. let's see the details about **LeafNodeData**.

### LeafNodeData

Fig.2：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/2.png">

#### Count

&emsp;&emsp; **Count** describes the number of point in a **LeafNodeData**.

#### DocIds

&emsp;&emsp; **DocIds** describes a set of document IDs which a point in a **LeafNodeData** belongs to.

#### PointValues

&emsp;&emsp; **PointValues** describes a set of point values, each point value is split into two parts: **CommonPrefixes** and **BlockPackedValues**.

##### CommonPrefixes

&emsp;&emsp; **CommonPrefixes** describes a set of common prefixes with each dimension.

Fig.3：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/3.png">

###### Length、Value

&emsp;&emsp; **Value** describes the common prefix value with byte and **Length** means the length of common prefix value.

##### BlockPackedValues

&emsp;&emsp; **BlockPackedValues** describes the suffix of point value. **BlockPackedValues** has two types of data structure according to the suffix. Due to the length of this article, it is not appropriate to extend the introduction.

## .kdi

&emsp;&emsp;The data structure of .kdi file as below: 

Fig.4：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/4.png">

### PackedIndexValue

&emsp;&emsp;**PackedIndexValue** describes the informations of all inner nodes. inner node has four types according to the position in KD-tree and its sub node type.

- RightNodeHasLeafChild:         the inner node is the right node of its parent node and its child nodes **are** leaf node
- RightNodeHasNotLeafChild:  the inner node is the right node of its parent node and its child nodes **are not** leaf node
- LeftNodeHasLeafChild:           the inner node is the left node of its parent node and its child nodes **are** leaf node
- LeftNodeHasNotLeafChild:     the inner node is the left node of its parent node and its child nodes **are not** leaf node

&emsp;&emsp;Root node is a inner node and it belongs to **RightNodeHasNotLeafChild**.

#### RightNodeHasNotLeafChild

Fig.5：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/5.png">

##### LeftLeafBlockFP

&emsp;&emsp;**LeftLeafBlockFP** describes a pointer where the position of the leftmost leaf node of current inner node in .kdd file is.

##### Code 

&emsp;&emsp;**Code** describes the metadata about inner node,  such as spilt dimension or bytes per dimension(How many bytes each value in each dimension takes).

##### SplitValue

&emsp;&emsp;In the stage of constructing a KD-tree, the set of points in a inner node will be split into two sub nodes according to **SpiltValue**

##### LeftNumBytes

&emsp;&emsp;**LeftNumBytes** describes the total size of one inner node's sub tree.

#### RightNodeHasNotLeafChild

Fig.6：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/6.png">

&emsp;&emsp;**LeftNumBytes**,  **Code** and **SplitValue** describes the same meaning as above.

##### RightLeafBlockFP

&emsp;&emsp;**RightLeafBlockFP** describes a pointer where the position of the right leaf node of current inner node in .kdd file is.

#### LeftNodeHasLeafChild

Fig.7：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/7.png">

&emsp;&emsp;**Code**, **SplitValue** and **RightLeafBlockFP** describe the same meaning as above.

#### LeftNodeHasNotLeafChild

Fig.8：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/8.png">

&emsp;&emsp;**Code**, **SplitValue** and **LeftNumBytes** describe the same meaning as above.

## KD-tree and Index File

&emsp;&emsp;Inner node in a KD-tree would be stored in .kid file as blow:

Fig.9：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/9.png">

[High-definition pictures](http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/innertree.html)

&emsp;&emsp;Leaf node in a KD-tree would be stored in .kdd file as blow:

Fig.10：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/10.png">

[High-definition pictures](http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/leaftree.html)

### **LeftNumBytes**

Fig.11：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/11.png">

[High-definition pictures](http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/leftnumbytes.html)

Fig.12：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/12.png">

[High-definition pictures](http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/leftnumbytes.html)

&emsp;&emsp;As above said, **LeftNumBytes** describes the total size of one inner node's  sub tree, it makes a reader can skip to the position of inner node's right node in .kid file and its left node is just in the next position.

### **LeftLeafBlockFP** **RightLeafBlockFP**

Fig.13：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/13.png">

[High-definition pictures](http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/leftleafblockfpandrightleafblockfp1.html)

Fig.14：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/14.png">

&emsp;&emsp;With **LeftLeafBlockFP** and **RightLeafBlockFP**, leaf Node can be positioned in .kdd file.

[High-definition pictures](http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/leftleafblockfpandrightleafblockfp2.html)

## .kdm

&emsp;&emsp;The data structure of .kdm file as below: 

Fig.15：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/15.png">

### - 1

&emsp;&emsp;**-1** is a fix padding value, In the stage of reading .kdm file, it as a mark means all the **Field**s has been readed.

### **KdiFileLength** **KddFileLength**

&emsp;&emsp;In the stage of reading index file, **KdiFileLength** and **KddFileLength** used to validate .kdi and .kdd files, such as truncated file or file too long. 

Fig.16：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/16.png">

### FieldNumber

&emsp;&emsp;**FieldNumber**  uniquely represents a **Field**.

### Index

Fig.17：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/17.png">

#### NumDataDims

&emsp;&emsp;**NumDataDims** describes how many dimensions we are storing at the leaf (data) nodes.

#### NumIndexDims

&emsp;&emsp;**NumIndexDims** describes how many dimensions we are indexing in the internal nodes.

#### CountPerLeaf

&emsp;&emsp;**CountPerLeaf** describes the maximum number of point in each leaf node.

#### BytesPerDim

&emsp;&emsp;**BytesPerDim** describes the number of bytes per dimension.

#### NumLeaves

&emsp;&emsp;**NumLeaves** describes the number of leaf node.

#### MinPackedValue MaxPackedValue

&emsp;&emsp;**MinPackedValue** describes minimum value for each dimension and **MaxPackedValue** describes maximum value for each dimension values.

#### PointCount

&emsp;&emsp;**PointCount** describes the total number of indexed points across all documents.

#### DocCount

&emsp;&emsp;**DocCount** describes the total number of documents that have indexed at least one point.

#### Length

&emsp;&emsp;**Length** describes the  size of **PackedIndexValue** in Fig.4.

#### DataStartFP

&emsp;&emsp;**DataStartFP** describes a pointer where the beginning position of current field data in .kdd file is.

#### IndexStartFP

&emsp;&emsp;**IndexStartFP** describes a pointer where the beginning position of current field data in .kdi file is.

Fig.18：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/18.png">

## Complete data structure

Fig.19：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/19.png">

Fig.20：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/20.png">

Fig.21：

<img src="http://www.amazingkoala.com.cn/uploads/luceneEnglish/IndexFile/kdd&kdi&kdm/21.png">















