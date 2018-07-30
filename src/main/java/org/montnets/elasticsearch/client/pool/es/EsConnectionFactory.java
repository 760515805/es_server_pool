package org.montnets.elasticsearch.client.pool.es;

import java.io.IOException;
import java.util.Objects;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestHighLevelClient;
import org.montnets.elasticsearch.client.pool.ConnectionFactory;
import org.montnets.elasticsearch.common.enums.EsConnect;
import org.montnets.elasticsearch.common.exception.ConnectionException;
import org.montnets.elasticsearch.config.EsConnectConfig;

/**
 * 
* Copyright: Copyright (c) 2018 Montnets
* 
* @ClassName: EsConnectionFactory.java
* @Description: ES连接工厂

* @version: v1.0.0
* @author: chenhj
* @date: 2018年7月26日 下午2:58:25 
*
* Modification History:
* Date         Author          Version            Description
*---------------------------------------------------------*
* 2018年7月26日     chenhj          v1.0.0               修改原因
 */
class EsConnectionFactory implements ConnectionFactory<RestHighLevelClient> {
    /**
	 *@Fields serialVersionUID : TODO
	 */
	private static final long serialVersionUID = 1L;
    /**
     * clusterName
     */
    private final String clusterName;
	/**
     * nodes
     */
    private HttpHost[] nodes;
    /******设置连接超时时间********/
    private final  int connectTimeoutMillis;
 	/******设置网络超时时间********/
    private final  int socketTimeoutMillis;
 	 /******设置连接请求超时时间********/
    private final int connectionRequestTimeoutMillis;
 	 /******设置ES连接超时时间********/
    private final  int maxRetryTimeoutMillis;
 	 /******设置路由连接最大数********/
    private final  int maxConnPerRoute;
 	 /******设置连接池大小********/
    private final  int maxConnTotal; 
    /**
     * username
     */
   // private final String username;
    /**
     * password
     */
  //  private final String password;

    public EsConnectionFactory(final EsConnectConfig esConfig) {
    	if(Objects.isNull(esConfig)){
    		throw new ConnectionException("[esConfig] can not null!");
    	}
        try {
			this.nodes =esConfig.getNodes();
		} catch (IllegalAccessException e) {
			 throw new ConnectionException("[nodes ip] is required !"+e);
		}
        if (Objects.isNull(nodes)||nodes.length==0){
        	throw new ConnectionException("[nodes ip] is required !");
        }
        this.clusterName=esConfig.getClusterName();
        if (Objects.isNull(clusterName)){
        	throw new ConnectionException("[clusterName] is required !");
        }
        this.connectTimeoutMillis=esConfig.getConnectTimeoutMillis();
        if (Objects.isNull(connectTimeoutMillis)){
        	throw new ConnectionException("[connectTimeoutMillis] is required !");
        }
        this.socketTimeoutMillis=esConfig.getSocketTimeoutMillis();
        if (Objects.isNull(socketTimeoutMillis)){
        	throw new ConnectionException("[socketTimeoutMillis] is required !");
        }
        this.connectionRequestTimeoutMillis=esConfig.getConnectionRequestTimeoutMillis();
        if (Objects.isNull(connectionRequestTimeoutMillis)){
        	throw new ConnectionException("[connectionRequestTimeoutMillis] is required !");
        }
        this.maxRetryTimeoutMillis=esConfig.getMaxRetryTimeoutMillis();
        if (Objects.isNull(maxRetryTimeoutMillis)){
        	throw new ConnectionException("[maxRetryTimeoutMillis] is required !");
        }
        this.maxConnPerRoute=esConfig.getMaxConnPerRoute();
        if (Objects.isNull(maxConnPerRoute)){
        	throw new ConnectionException("[maxConnPerRoute] is required !");
        }
        this.maxConnTotal=esConfig.getMaxConnTotal();
        if (Objects.isNull(maxConnTotal)){
        	throw new ConnectionException("[maxConnTotal] is required !");
        }
     //   this.username =esConfig.getUsername();
     //  if (username == null)
          //  throw new ConnectionException("[username] is required !");
      //  this.password = esConfig.getPassword();
      //  if (password == null)
          //  throw new ConnectionException("[password] is required !");
    }
	/**
	 * 当对象池中没有多余的对象可以用的时候，调用此方法。
	 */
    @Override
    public PooledObject<RestHighLevelClient> makeObject() throws Exception {
    	RestHighLevelClient client = this.createConnection();
        return new DefaultPooledObject<RestHighLevelClient>(client);
    }
    /**
     * 销毁被破坏的实例  
     */
    @Override
    public void destroyObject(PooledObject<RestHighLevelClient> p) throws Exception {
    	RestHighLevelClient client = p.getObject();
        if (Objects.nonNull(client)){
        	client.close();
        }
    }
    /**
     * 功能描述：判断资源对象是否有效，有效返回 true，无效返回 false
     * 
     * 什么时候会调用此方法
     * 1：从资源池中获取资源的时候，参数 testOnBorrow 或者 testOnCreate 中有一个 配置 为 true 时，则调用  factory.validateObject() 方法
     * 2：将资源返还给资源池的时候，参数 testOnReturn，配置为 true 时，调用此方法
     * 3：资源回收线程，回收资源的时候，参数 testWhileIdle，配置为 true 时，调用此方法
     */
    @Override
    public boolean validateObject(PooledObject<RestHighLevelClient> p) {
    	RestHighLevelClient client = p.getObject();
    	if (client != null){
        	try {
				return client.ping(EsConnect.EMPTY_HEADERS);
			} catch (IOException e) {
				throw new ConnectionException("连接失效!");
			}
        }
        return false;
    }
	/**
	    * 功能描述：激活资源对象
		* 什么时候会调用此方法
		* 1：从资源池中获取资源的时候
		* 2：资源回收线程，回收资源的时候，根据配置的 testWhileIdle 参数，判断 是否执行 factory.activateObject()方法，true 执行，false 不执行
	 */
    @Override
    public void activateObject(PooledObject<RestHighLevelClient> p) throws Exception {

    }
    /**
     * 取消初始化实例返回到空闲对象池 
	 * 功能描述：钝化资源对象
	 * 什么时候会调用此方法
	 * 1：将资源返还给资源池时，调用此方法。
	 */
    @Override
    public void passivateObject(PooledObject<RestHighLevelClient> p) throws Exception {
       
    }
    @Override
    public RestHighLevelClient createConnection() throws Exception {
    	//加载配置
    	RestClientFactory factory = new RestClientFactory(connectTimeoutMillis, socketTimeoutMillis, connectionRequestTimeoutMillis, 
    			maxRetryTimeoutMillis, maxConnTotal, maxConnPerRoute, nodes);
    	//初始化
    	factory.init();
    	RestHighLevelClient client = factory.getRhlClient();
        return client;
    }
}
