package com.beeasy.web.core;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.beeasy.web.core.boost.SqlBoost;
import io.netty.handler.codec.http.FullHttpRequest;
import org.beetl.sql.core.annotatoin.Table;
import org.beetl.sql.core.engine.PageQuery;
import org.beetl.sql.core.query.LambdaQuery;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import static cn.hutool.core.util.StrUtil.*;
import static com.beeasy.web.core.Config.config;
import static com.beeasy.web.core.DBService.sqlManager;

public class Param {
    private static List<Rule> ruleList = new Vector<>();

    public static Object[] AutoWiredParams(FullHttpRequest request, Class clz, Method method, Map<Class, Object> staticArgs) {
        var context = Context.holder.get();
        Parameter[] parameters = method.getParameters();
        Object[] ret = new Object[parameters.length];
        int idex = -1;
        int i = 0;

        for (Parameter parameter : parameters) {
            //特殊字段
            String name = parameter.getName();
            Class<?> type = parameter.getType();
//            name = names.get(i);
            ret[i] = null;
            //如果有静态，则直接使用
            if (null != staticArgs && staticArgs.containsKey(type)) {
                ret[i++] = staticArgs.get(type);
                continue;
            }

            if(type == Cookie.class){
                ret[i++] = context.cookie;
                continue;
            }
            if(type == FullHttpRequest.class){
                ret[i++] = request;
                continue;
            }

            switch (name) {
                case "query":
//                    ret[i] = JSON.parseObject(JSON.toJSONBytes(context.query), Obj.class);
//                    ret[i] = (Obj)context.query.toJavaObject(new TypeReference<Obj>(){});
                    ret[i] = context.query.toJavaObject(type);
                    break;

                case "body":
                    if (context.body != null) {
                        ret[i] = context.body.toJavaObject(type);
                    }
                    break;

                case "params":
                    ret[i] = context.params.toJavaObject(type);
                    break;

                case "headers":
                    ret[i] = context.headers.toJavaObject(type);
                    break;

                default:
                    //必然为JSONOBJECT
                    idex = type.getTypeName().indexOf("[]");
                    //是数组的情况
                    if (idex > -1) {
                        String source = context.query.getString(name);
                        if (isNotEmpty(source)) {
                            if (source.startsWith("[") && source.endsWith("]")) {
                                JSONArray array = context.query.getJSONArray(name);
                                ret[i] = array.toJavaObject(type);
                            }
                            //只有一个的情况，直接尝试拆分
                            else {
//                                if (source.contains(",")) {
                                String[] split = source.split(",");
                                JSONArray array = new JSONArray(Arrays.asList(split));
                                try {
                                    ret[i] = array.toJavaObject(type);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else if (type == MultipartFile.class) {
                        ret[i] = context.params.get(name);
                    } else if (type == Context.class){
                        ret[i] = context;
                    }
                    else{
                        /**
                         * 从这里开始，因为类是动态加载的，所以对自定义的类要判定名字
                         */
                        Action action = null;
                        if(PageQuery.class.isAssignableFrom(type)){
                            var gen = getGenericType(parameter);
                            if (isModel(parameter, gen)) {
                                action = (ctx, p, ac) -> pageQuery(ctx,p, gen, 0);
                            }
                        }
                        else if(Collection.class.isAssignableFrom(type)){
                            var gen = getGenericType(parameter);
                            if(isModel(parameter, gen)){
                                action = (ctx,p,ac) -> pageQuery(ctx,p, gen, 1);
                            }
                        }
                        else if(isModel(parameter, parameter.getType())){
                            action = (ctx,p,ac) -> pageQuery(ctx, p, parameter.getType(), 2);
                        }
                        rule:{
                            for (Rule rule : ruleList) {
                                if(rule.condition.canRule(context, parameter)){
                                    ret[i] = rule.action.around(context, parameter, action);
                                    break rule;
                                }
                            }
                            if (action != null) {
                                ret[i] = action.around(context, parameter, null);
                            }
                        }
                    }
                    if (ret[i] == null) {
                        ret[i] = context.getParamValue(name, type);
                    }
                    break;
            }
            i++;
        }
        return ret;
    }


    private static Class getGenericType(Parameter parameter){
        var p = (ParameterizedType)parameter.getParameterizedType();
        return (Class) p.getActualTypeArguments()[0];
    }

    private static boolean isModel(Parameter p, Class clz){
        return (config.model != null && config.model.stream()
            .anyMatch(e -> clz.getName().startsWith(e))) || (p.getAnnotation(SqlBoost.class) != null);
    }


    private static Object pageQuery(Context context, Parameter parameter, Class gType, int flag){
        //如果这个字段有注解
        SqlBoost boost = parameter.getAnnotation(SqlBoost.class);
        String as = "b";
        Class retType = gType;
        if (boost != null && boost.model().length > 0) {
            retType = boost.model()[0];
        }

        //查看有没有字段
        var q = sqlManager.lambdaQuery(retType);
        q.appendSql(" as " + as + " ");

        var fields = new HashMap<String, Field>();
        for (Field field : retType.getDeclaredFields()) {
            if(Modifier.isStatic(field.getModifiers())){
                continue;
            }
            fields.put(field.getName(), field);
        }
        var page = 1;
        var size = 10;
//        q.appendSql("m left join ra_store store on m.member_id = store.member_id ");

        for (Map.Entry<String, Object> entry : context.query.entrySet()) {
            if(flag == 0){
                if(entry.getKey().equalsIgnoreCase("page")) {
                    try{
                        page = Integer.parseUnsignedInt(String.valueOf(entry.getValue())) ;
                    } catch (Exception e){
                    }
                } else if(entry.getKey().equalsIgnoreCase("size")) {
                    try {
                        size = Integer.parseUnsignedInt(String.valueOf(entry.getValue()));
                    } catch (Exception e) {
                    }
                }
            }
            //判断有没有这个字段
            var f = fields.get(entry.getKey());
            if (f == null) {
                continue;
            }
            if (StrUtil.isNotBlank(String.valueOf(entry.getValue()))) {
                q.andEq((as + ".") + f.getName(), entry.getValue());
            }
        }

        //补充关联字段
        Table table = (Table) retType.getAnnotation(Table.class);
        List<Config.Link> links = new ArrayList<>();

        var nfs = fields.keySet()
            .stream()
            .map(e -> as + "." + e)
            .collect(Collectors.toSet());
        if (boost != null && boost.appendField().length > 0) {
            var key = table.name();
            var idex = key.indexOf(".");
            if(idex > -1){
                key = key.substring(idex + 1);
            }
            for (String s : boost.appendField()) {
                if(StrUtil.isEmpty(s)){
                    continue;
                }
                var link = config.links.get(key + s);
                if (link == null) {
                    nfs.add(s);
                } else {
                    links.add(link);
                }
            }
        }


        if(flag == 0){
            var ret = q.page(page, size, gType, nfs.toArray(String[]::new));
            addLinks(ret.getList(), links);
            return ret;
        } else if(flag == 1){
            var ret = q.select(gType, nfs.toArray(String[]::new));
            addLinks(ret, links);
            return ret;
        } else if(flag == 2){
            if(JSONObject.class.isAssignableFrom(gType)){
                var ret =  JSON.toJSON(q.single());
                addLinks(Arrays.asList((JSONObject)ret), links);
                return ret;
            } else {
                var ret = q.single();
                return ret;
            }
        } else {
            return null;
        }
    }

    private static void addLinks(Collection<JSONObject> ret, List<Config.Link> links){
        for (Config.Link link : links) {
            var map = new JSONObject();
            var ids = ret
                .stream()
                .peek(e -> map.put(((JSONObject) e).getString(link.fromField), e))
                .map(e -> ((JSONObject) e).getString(link.fromField))
                .map(e -> StrUtil.wrap((CharSequence) e, "'"))
                .collect(Collectors.joining(","));
            if(StrUtil.isEmpty((CharSequence) ids)){
                continue;
            }
            var items = sqlManager.execute(String.format("select * from %s where %s in (%s)", link.toClz, link.toField, ids), JSONObject.class, new JSONObject());
            for (JSONObject item : items) {
                var target = map.getJSONObject(item.getString(link.toField));
                if (target == null) {
                    continue;
                }
                Object arr = target.get(link.name);
                if (arr == null) {
                    if(link.many){
                        arr = new JSONArray();
                    } else {
                        arr = new JSONObject();
                    }
                    target.put(link.name, arr);
                }
                if(link.many){
                    ((JSONArray)arr).add(item);
                } else {
                    ((JSONObject)arr).putAll(item);
                }
            }
        }
    }

    public static void AddRule(Condition condition,  Action action){
        ruleList.add(new Rule(condition,  action));
    }


    public static interface Condition{
        boolean canRule(Context context, Parameter parameter);
    }

    public static interface Action{
        Object around(Context context, Parameter parameter, Action defaultAction);
    }

    public static class Rule{
        public Condition condition;
        public Action action;

        public Rule(Condition condition, Action action) {
            this.condition = condition;
            this.action = action;
        }
    }


}
