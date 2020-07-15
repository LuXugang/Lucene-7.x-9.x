package io;


//import org.apache.http.HttpHost;
//import org.elasticsearch.action.index.IndexRequest;
//import org.elasticsearch.action.index.IndexResponse;
//import org.elasticsearch.client.RequestOptions;
//import org.elasticsearch.client.Requests;
//import org.elasticsearch.client.RestClient;
//import org.elasticsearch.client.RestHighLevelClient;
//import org.elasticsearch.client.indices.CreateIndexRequest;
//import org.elasticsearch.client.indices.CreateIndexResponse;
//import org.elasticsearch.common.settings.Settings;
//import org.elasticsearch.common.xcontent.XContentType;
//
//import java.util.HashMap;
//import java.util.Map;

/**
 * @author Lu Xugang
 * @date 2020/7/8 4:34 下午
 */
public class RestClientTest {
//
//    public static void main(String[] args) throws Exception{
//        try(RestHighLevelClient client = new RestHighLevelClient(
//                RestClient.builder(new HttpHost("192.168.101.102", 9200, "http")));) {
//
//            String indexName = "myxinde5";
//            CreateIndexRequest request = new CreateIndexRequest(indexName);
//            request.settings(Settings.builder()
//                    .put("index.number_of_shards", 1)
//                    .put("index.number_of_replicas", 1)
//            );
//
//            // 第一种
//            request.mapping("{\"properties\":{\"msg\":{\"type\":\"text\"}}}", XContentType.JSON);
//
//            // 第二种
//            Map<String, Object> properties = new HashMap<>();// properties
//
//            Map<String, Object> message = new HashMap<>();
//            message.put("type", "text");
//            properties.put("message", message);// 添加映射
//            Map<String, Object> username = new HashMap<>();
//            username.put("type", "text");
//            properties.put("username", username);// 添加映射
//
//            Map<String, Object> mapping = new HashMap<>();
//            mapping.put("properties", properties);
//            request.mapping(mapping);
//
//            CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
//            System.out.println(createIndexResponse.index());
//            int count = 1;
//            add("hope fro the future test ? ccc ddd ? eee", "? ccc ddd", client, count++, indexName);
//            add("? aaa ddd ? eee", "test ? ccc ddd ? eee", client, count++, indexName);
//
//        }
//
//
//    }
//
//    public static void add(String userName, String message,RestHighLevelClient client,int id, String index) throws Exception{
//        IndexRequest indexRequest = Requests.indexRequest(index);
//        indexRequest.id(String.valueOf(id));// 指定ID
//        indexRequest.source("message", message,
//                "username", userName);// 支持多种方式
//        IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
//        System.out.println(indexResponse);
//    }
}
