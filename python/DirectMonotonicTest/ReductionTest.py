# This is a sample Python script.

# Press ⇧F10 to execute it or replace it with your code.
# Press Double ⇧ to search everywhere for classes, files, tool windows, actions, and settings.
import matplotlib.pyplot as plt
import numpy as np
import random


def print_hi(name):
    # Use a breakpoint in the code line below to debug your script.
    print(f'Hi, {name}')  # Press ⌘F8 to toggle the breakpoint.


# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    x = []
    buffer = []
    numDoc = 0
    for i in range(0, 1024):
        x.append(i)
        buffer.append(numDoc)
        numDoc += random.randint(1, 128)
        # 等差数列
        # numDoc += 128
    plt.plot(x, buffer)
    plt.show()

    # 步骤一：计算平均值avgInc
    avgInc = (buffer[1023] - buffer[0]) / (len(buffer) - 1)

    # 步骤二：缩放数据
    firstEncodeArray = []
    index = 0
    for element in buffer:
        firstEncodeArray.append(element - avgInc * index)
        index += 1
    plt.plot(x, firstEncodeArray)
    plt.show()

    # 步骤三：计算最小值minValue
    minValue = firstEncodeArray[0]
    for element in firstEncodeArray:
        minValue = min(minValue, element)
    print(minValue)

    # 步骤四：无符号处理
    secondEncodeArray = []
    for element in firstEncodeArray:
        secondEncodeArray.append(element - minValue)
    plt.plot(x, secondEncodeArray)
    plt.show()
# See PyCharm help at https://www.jetbrains.com/help/pycharm/
