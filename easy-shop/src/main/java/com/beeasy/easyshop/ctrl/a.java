package com.beeasy.easyshop.ctrl;


import com.alibaba.fastjson.JSONObject;
import com.beeasy.easyshop.model.RaAlbumClass;
import com.beeasy.web.core.MultipartFile;
import com.beeasy.web.core.boost.SqlBoost;
import org.beetl.sql.core.SQLReady;
import org.beetl.sql.core.engine.PageQuery;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.beeasy.web.core.DBService.sqlManager;

public class a {

    public String b(){
        return "aaa 见证奇迹的时刻";
    }

    public String test2(int c){
        return c + " fffffffffffffffffff";
    }

    public Object test3(JSONObject query){
        return query;
    }
    public Object test4(test query){
        query.b = "rilegou";
        return query;
    }

    public Object test5(JSONObject body){
        return body;
    }

    public Object test6(JSONObject params){
        return params;
    }

    //file upload
    public Object test7(
            MultipartFile file
    ) throws IOException {
        file.transferTo(new File("e:/rilegou.txt"));
        return "ok";
    }

    public Object test8(){
        return sqlManager.execute(new SQLReady("select count(1) from ra_member"), JSONObject.class);
    }


    public static class test{
        public String b;
        public String c;
        public String d;
    }


    /***************************************/


    public Object tests1(List<RaAlbumClass> list){
        return list;
    }

    //查我的相册
    public Object tests2(PageQuery<RaAlbumClass> mylist){
        return mylist;
    }

    //有bug就不演示了
    //通常 查询中需要补充字段，例如我的相册里我还要知道每个相册有多少图片
    // 因为补充字段，所以不能再用这个实体了
    public Object tests3(
            //这里model声明真正的类型
            @SqlBoost(model = RaAlbumClass.class, appendField = "(select count(1) from ra_album_pic where aclass_id = b.aclass_id) as total") PageQuery<JSONObject> mylist //这里因为total不属于Raalbumclass的字段  所以只能用一个map来存储
    ){
        return mylist;
    }
}
