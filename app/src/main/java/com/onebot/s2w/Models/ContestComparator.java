package com.onebot.s2w.Models;

import java.util.Comparator;

public class ContestComparator implements Comparator<Contest>
    {
        public int compare(Contest left, Contest right) {
            int result = 0;
            if (left.numLikes < right.numLikes)
                result = 1;
            else if (left.numLikes > right.numLikes)
                result = -1;
            return result;
        }
    }
