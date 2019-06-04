package com.beeasy.easyshop.filter;

import com.beeasy.web.core.AopInvoke;

public class auth{

    public Object around(AopInvoke invoke, int c) throws Exception {
        if(c == 1){
            return "被拦截掉";
        }
        //pass
        return invoke.call();
    }
}