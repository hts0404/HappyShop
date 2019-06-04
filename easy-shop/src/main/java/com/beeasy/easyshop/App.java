package com.beeasy.easyshop;

import com.beeasy.web.core.DBService;
import com.beeasy.web.core.Nami;
import com.beeasy.web.core.Param;
import org.beetl.sql.core.NameConversion;
import org.beetl.sql.core.engine.PageQuery;

public class App {

    public static void main(String[] args) {

        Param.AddRule((ctx,param) -> {
            //是否操作注入的条件
            //如果被注入的参数是pagequery
            if(PageQuery.class.isAssignableFrom(param.getType())){
                //如果参数名以my开头
                if(param.getName().startsWith("my")){
                    return true;
                }
            }
            return false;

        },(ctx, param, action) -> {
            //操作注入的行为
            ctx.query.put("store_id", 6); //这个6通常是通过权限验证拿到的我的店铺的ID，这里写死他
            return action.around(ctx, param, null);
        });

        Nami.start();
    }

}
