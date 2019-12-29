package com.google.vr.sdk.samples.hellovr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class Scene {
    public char[][][] scene = {
            {   //0层
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,31},
                    {31, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,31},
                    {31, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,31},
                    {31, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,31},
                    {31, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,31},
                    {31, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,31},
                    {31, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,31},
                    {31, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,31},
                    {31, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,31},
                    {31, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,31},
                    {31, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,31},
                    {31, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,31},
                    {31, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,31},
                    {31, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,31},
                    {31, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
            },
            {   //1层
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,31},
                    {31, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,31},
                    {31, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,31},
                    {31, 1, 1, 1, 14,14,14,14,14,14,14,14,14,1, 1, 1,31},
                    {31, 1, 1, 1, 14,26,26,26,26,26,26,26,14,1, 1, 1,31},
                    {31, 1, 1, 1, 14,26,13,13,13,13,13,26,14,1, 1, 1,31},
                    {31, 1, 1, 1, 14,26,13,12,12,12,13,26,14,1, 1, 1,31},
                    {31, 1, 1, 1, 14,26,13,12,15,12,13,26,14,1, 1, 1,31},
                    {31, 1, 1, 1, 14,26,13,12,12,12,13,26,14,1, 1, 1,31},
                    {31, 1, 1, 1, 14,26,13,13,13,13,13,26,14,1, 1, 1,31},
                    {31, 1, 1, 1, 14,26,26,26,26,26,26,26,14,1, 1, 1,31},
                    {31, 1, 1, 1, 14,14,14,14,14,14,14,14,14,1, 1, 1,31},
                    {31, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,31},
                    {31, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,31},
                    {31, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31}
            },
            {   //2层
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,31},
                    {31, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,31},
                    {31, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,31},
                    {31, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1,31},
                    {31, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1,31},
                    {31, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1,31},
                    {31, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1,31},
                    {31, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1,31},
                    {31, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1,31},
                    {31, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1,31},
                    {31, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1,31},
                    {31, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1,31},
                    {31, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,31},
                    {31, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,31},
                    {31, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31}
            },
            {   //3层
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,31},
                    {31, 1, 21,21,21,21,21,21,21,21,21,21,21,21,21,1,31},
                    {31, 1, 21, 0, 0,10,10, 9, 9, 9,10,10, 0, 0,21,1,31},
                    {31, 1, 21, 0, 0,10,20,20,20,20,20,10, 0, 0,21,1,31},
                    {31, 1, 21, 0, 0,10,19, 0, 0, 0,27,10, 0, 0,21,1,31},
                    {31, 1, 21, 0, 0,10,19, 0, 0, 0,27,10, 0, 0,21,1,31},
                    {31, 1, 21, 0, 0,10, 0, 0, 0, 0, 0,10, 0, 0,21,1,31},
                    {31, 1, 21, 0, 0,10,10,10, 0,10,10,10, 0, 0,21,1,31},
                    {31, 1, 21, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,21,1,31},
                    {31, 1, 21, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,21,1,31},
                    {31, 1, 21, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,21,1,31},
                    {31, 1, 21, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0,21,1,31},
                    {31, 1, 21, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,21,1,31},
                    {31, 1, 21,21,21,21,21,21,21,21,21,21,21,21,21,1,31},
                    {31, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31}
            },
            {   //4层
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,31},
                    {31, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7,31},
                    {31, 7, 0, 0, 0,10,10, 9, 9, 9,10,10, 0, 0, 0, 7,31},
                    {31, 7, 0, 0, 0,10, 6, 0,28, 0, 6,10, 0, 0, 0, 7,31},
                    {31, 7, 0, 0, 0,10,17, 0, 0, 0,18,10, 0, 0, 0, 7,31},
                    {31, 7, 0, 0, 0,10,17, 0, 0, 0,18,10, 0, 0, 0, 7,31},
                    {31, 7, 0, 0, 0,10,25, 0, 0, 0,25,10, 0, 0, 0, 7,31},
                    {31, 7, 0, 0, 0,10,10,10, 0,10,10,10, 0, 0, 0, 7,31},
                    {31, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7,31},
                    {31, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7,31},
                    {31, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7,31},
                    {31, 7, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 7,31},
                    {31, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7,31},
                    {31, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7,31},
                    {31, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31}
            },
            {   //5层
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31, 6, 0, 0, 0, 6, 0, 0,16, 0, 0, 6, 0, 0, 0, 6,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0,10,10,10,10,10,10,10, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0,10,10,10,10,10,10,10, 0, 0, 0, 0,31},
                    {31, 6, 0, 0, 0,10,10,10,10,10,10,10, 0, 0, 0, 6,31},
                    {31, 0, 0, 0, 0,10,10,10,10,10,10,10, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0,10,10,10,10,10,10,10, 0, 0, 0, 0,31},
                    {31,16, 0, 0, 0,10,10,10,10,10,10,10, 0, 0, 0,16,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 5, 5, 5, 0, 0, 0, 0, 0, 0,31},
                    {31, 6, 0, 0, 0, 0, 5, 5, 5, 5, 5, 0, 0, 0, 0, 6,31},
                    {31, 0, 0, 0, 0, 0, 5, 5, 4, 5, 5, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 5, 5, 5, 5, 5, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 5, 5, 5, 0, 0, 0, 0, 0, 0,31},
                    {31, 6, 0, 0, 0, 6, 0, 0,16, 0, 0, 6, 0, 0, 0, 6,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31}
            },
            {   //6层
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31, 0, 0, 0, 0, 0,11,11,11,11,11, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0,11,22,11, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0 ,0 ,0 ,0 ,0 ,0 ,0 ,0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0 ,0 ,0 ,0 ,0 ,0 ,0 ,0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0 ,0 ,0 ,0 ,0 ,0 ,0 ,0, 0, 0, 0, 0,31},
                    {31,23, 0, 0, 0 ,0 ,0 ,0 ,0 ,0 ,0 ,0, 0, 0, 0,24,31},
                    {31,23,23, 0, 0 ,0 ,0 ,0 ,0 ,0 ,0 ,0, 0, 0,24,24,31},
                    {31,23,29, 0, 0 ,0 ,0 ,0 ,0 ,0 ,0 ,0, 0, 0,30,24,31},
                    {31,23,23, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,24,24,31},
                    {31,23, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,24,31},
                    {31, 0, 0, 0, 0, 0, 0, 5, 5, 5, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 5, 4, 5, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 5, 5, 5, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31}
            },
            {   //7层
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 5, 5, 5, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31}
            },
            {   //8层
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31}
            },
            {   //9层
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31}
            },
            {   //10层
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31},
                    {31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31,31}
            },
    };
    
    Scene(InputStream in)
    {
        BufferedReader reader=new BufferedReader(new InputStreamReader(in));
        char[][][]scene=new char[HelloVrActivity.TOP_LIMIT-HelloVrActivity.BOTTOM_LIMIT][HelloVrActivity.EAST_LIMIT-HelloVrActivity.WEST_LIMIT][HelloVrActivity.NORTH_LIMIT-HelloVrActivity.SOUTH_LIMIT];
        for(int i=0;i<HelloVrActivity.EAST_LIMIT-HelloVrActivity.WEST_LIMIT;i++)
        {
            StringTokenizer tokenizer;
            try
            {
                tokenizer=new StringTokenizer(reader.readLine());
            }catch(IOException e){throw new RuntimeException(e);}
            for(int j=0;j<HelloVrActivity.NORTH_LIMIT-HelloVrActivity.SOUTH_LIMIT;j++)
            {
                int stoneHeight=(int)(Float.parseFloat(tokenizer.nextToken())*4+1);
                int dirtHeight=(int)(Float.parseFloat(tokenizer.nextToken())*3+3);
                if(i>=this.scene[0].length||j>=this.scene[0][0].length)
                {
                    for(int k=0;k<stoneHeight;k++)
                        scene[k][i][j]=1;
                    for(int k=stoneHeight;k<dirtHeight;k++)
                        scene[k][i][j]=10;
                    scene[dirtHeight][i][j]=2;
                }
                else
                {
                    for(int k=0;k<this.scene.length;k++)
                        scene[k][i][j]=this.scene[k][i][j];
                }
            }
        }
        this.scene=scene;
    }

    enum Position{
        NORTH,SOUTH,WEST,EAST,UP,DOWN
    }

    public int get_id(int i,int j,int k){
        return scene[i][j][k];
    }

    //获取块(i,j,k)某个方向的相邻方块ID
    public int get_neighbor_block_id(int i,int j,int k,Position p){
        switch (p){
            case UP:
                return (i+1>=scene.length)?0:scene[i+1][j][k];
            case DOWN:
                return (i-1<0)?0:scene[i-1][j][k];
            case EAST:
                return (j-1<0)?0:scene[i][j-1][k];
            case WEST:
                return (j+1>=scene[0].length)?0:scene[i][j+1][k];
            case NORTH:
                return (k-1<0)?0:scene[i][j][k-1];
            case SOUTH:
                return (k+1>=scene[0][0].length)?0:scene[i][j][k+1];
        }
        return -1;
    }

    public int get_scene_height(){
        return scene.length;
    }

    public int get_scene_width_ns(){
        return scene[0].length;
    }

    public int get_scene_width_we(){
        return scene[0][0].length;
    }

    public class Point{
        double x,y,z;
        public Point(double x,double y,double z){
            this.x=x;this.y=y;this.z=z;
        }
    }
    //坐标变换，由场景数组的(i,j,k)变到渲染引擎的(x,y,z)
    public Point transform_array_to_render(int scene_i, int scene_j, int scene_k){
        return new Point(get_scene_width_ns()-scene_j-1,get_scene_width_we()-scene_k-1, scene_i);
    }

    public Point transform_render_to_array(double render_x, double render_y, double render_z){
        return new Point(render_z,get_scene_width_ns()-render_x-1, get_scene_width_we()-render_y-1);
    }

    //坐标变换，由sdk的(x,y,z)变到渲染引擎的(x,y,z)
    public Point transform_sdk_to_render(double x,double y,double z){
        return new Point(-z,-x,y);
    }

    public Point transform_render_to_sdk(double x,double y,double z){
        return new Point(-y,z,-x);
    }
}
