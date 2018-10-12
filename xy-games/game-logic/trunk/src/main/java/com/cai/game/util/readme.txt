整体思路：
1、分而治之：检查手牌的所有花色，是否能组成3*n或3*n+2的牌型；
2、判断单一花色是否是3*n或3*n+2的牌型时，直接根据癞子个数、是否带将，去相应的表里查询是否包含牌型对应的整型值，存在即符合；
3、根据是否带将、是否风牌花牌、根据癞子个数，一共生成36张表，具体表名如下所示：
 癞子个数     带将         不带将        字牌带将         字牌不带将
    0      eye_table_0    table_0    feng_eye_table_0    feng_table_0
    1      eye_table_1    table_1    feng_eye_table_1    feng_table_1
    2      eye_table_2    table_2    feng_eye_table_2    feng_table_2
    3      eye_table_3    table_3    feng_eye_table_3    feng_table_3
    4      eye_table_4    table_4    feng_eye_table_4    feng_table_4
    5      eye_table_5    table_5    feng_eye_table_5    feng_table_5
    6      eye_table_6    table_6    feng_eye_table_6    feng_table_6
    7      eye_table_7    table_7    feng_eye_table_7    feng_table_7
    8      eye_table_8    table_8    feng_eye_table_8    feng_table_8

分析步骤：
1、统计手牌中鬼牌的个数，将鬼牌从牌数据中去除；
2、对不同的花色，分别校验是否能满足‘将、顺子、刻子’的胡牌类型；
3、分析字牌时，不需要分析包含‘顺子’的情况（如果风牌花牌能吃就另说）；
4、分析单一花色时，直接根据每张牌的点数1-9，生成一个对应的9位数的整型值，值的每一位上的数字0-4，代表该牌值点数的牌有几张；
    比如：1筒2筒3筒3筒3筒3筒6筒7筒8筒2万3万3万3万4万
    筒：1,1,4,0,0,1,1,1,0；得出的数字为114001110；
    万：0,1,3,1,0,0,0,0,0；得出的数字为13100000；
5、组合所有花色的牌，判断是否能胡牌。
6、将癞子分配给不同花色，分配方案直接穷举，只要有一种分配方案能让所有花色的牌构成胡牌牌型，则手牌能胡。

检查牌型时，每种花色的牌数量必须满足3*n或3*n+2的牌型，然后根据癞子个数、是否带将，查找相应的表，看能否满足3*n或3*n+2
的牌型，在表中找到相应的key值就表示能满足。

万条筒，表生成步骤：
1、穷举一种花色的所有能满足胡牌的牌型，将对应的牌型记录为一个整型值，将这个整型值添加到HashSet
    数据结构中（如果存在就不会添加），根据是否有将，将值放入对应的with_eye_0.data文件或with_no_eye_0.data文件。
    穷举时，每次加入一个将牌或刻子或顺子；穷举时，最多加入四组牌，外带将牌；
2、将with_eye_0.data文件中的牌型（一个牌型一个整型值），去掉一张牌，并将新牌型生成的整型值，放入with_
    eye_1.data文件中（去掉一张牌，表示这张牌用一张癞子牌代替）。同样的，with_no_eye_0.data文件也做类似的处理；
3、之后的几个表文件，以此类推，进行类似处理，一直到with_no_eye_8.data文件。

字牌，表生成步骤：
和万条筒的表生成步骤一样，只是在第一步的穷举中，不能加入顺子（除非玩法规则里，风牌能组成顺子）。

新增对对胡(碰碰胡)：
 癞子个数       带将           不带将            字牌带将           字牌不带将
    0      ph_eye_table_0    ph_table_0    ph_feng_eye_table_0    ph_feng_table_0
    1      ph_eye_table_1    ph_table_1    ph_feng_eye_table_1    ph_feng_table_1
    2      ph_eye_table_2    ph_table_2    ph_feng_eye_table_2    ph_feng_table_2
    3      ph_eye_table_3    ph_table_3    ph_feng_eye_table_2    ph_feng_table_2
    4      ph_eye_table_4    ph_table_4    ph_feng_eye_table_4    ph_feng_table_4
    5      ph_eye_table_5    ph_table_5    ph_feng_eye_table_5    ph_feng_table_5
    6      ph_eye_table_6    ph_table_6    ph_feng_eye_table_6    ph_feng_table_6
    7      ph_eye_table_7    ph_table_7    ph_feng_eye_table_7    ph_feng_table_7
    8      ph_eye_table_8    ph_table_8    ph_feng_eye_table_8    ph_feng_table_8

新增258将：
 癞子个数        带将           不带将          字牌不带将
    0      ewb_eye_table_0    ewb_table_0    ewb_feng_table_0
    1      ewb_eye_table_1    ewb_table_1    ewb_feng_table_1
    2      ewb_eye_table_2    ewb_table_2    ewb_feng_table_2
    3      ewb_eye_table_3    ewb_table_3    ewb_feng_table_2
    4      ewb_eye_table_4    ewb_table_4    ewb_feng_table_4
    5      ewb_eye_table_5    ewb_table_5    ewb_feng_table_5
    6      ewb_eye_table_6    ewb_table_6    ewb_feng_table_6
    7      ewb_eye_table_7    ewb_table_7    ewb_feng_table_7
    8      ewb_eye_table_8    ewb_table_8    ewb_feng_table_8

新增风牌能吃的表：(东南西北4种黑色风牌的任意不同的三张可以自由组合，
                     中发白3种非黑色风牌的任意不同的三张可以自由组合)
 癞子个数          带将                字牌不带将
    0      feng_chi_eye_table_0    feng_chi_table_0
    1      feng_chi_eye_table_1    feng_chi_table_1
    2      feng_chi_eye_table_2    feng_chi_table_2
    3      feng_chi_eye_table_3    feng_chi_table_2
    4      feng_chi_eye_table_4    feng_chi_table_4
    5      feng_chi_eye_table_5    feng_chi_table_5
    6      feng_chi_eye_table_6    feng_chi_table_6
    7      feng_chi_eye_table_7    feng_chi_table_7
    8      feng_chi_eye_table_8    feng_chi_table_8


新增有东风令是，风牌能吃的表：(东南西、东南北、东西北进行组合，或刻子进行组合；
                     中发白进行组合，和刻子进行组合)
 癞子个数             带将                  字牌不带将
    0      feng_chi_dfl_eye_table_0    feng_chi_dfl_table_0
    1      feng_chi_dfl_eye_table_1    feng_chi_dfl_table_1
    2      feng_chi_dfl_eye_table_2    feng_chi_dfl_table_2
    3      feng_chi_dfl_eye_table_3    feng_chi_dfl_table_2
    4      feng_chi_dfl_eye_table_4    feng_chi_dfl_table_4
    5      feng_chi_dfl_eye_table_5    feng_chi_dfl_table_5
    6      feng_chi_dfl_eye_table_6    feng_chi_dfl_table_6
    7      feng_chi_dfl_eye_table_7    feng_chi_dfl_table_7
    8      feng_chi_dfl_eye_table_8    feng_chi_dfl_table_8