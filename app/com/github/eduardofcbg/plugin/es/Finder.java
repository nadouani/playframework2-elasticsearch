package com.github.eduardofcbg.plugin.es;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;

import play.libs.F;
import play.libs.F.Promise;
import play.libs.F.RedeemablePromise;
import scala.concurrent.Future;

import com.fasterxml.jackson.core.JsonProcessingException;

@SuppressWarnings("rawtypes") 
public class Finder <T extends Index> {

    private Class<T> from;

    public Finder(Class<T> from) {
        this.from = from;
    }
            
    public Promise<Result> async(ActionRequestBuilder builder) {
    	return F.Promise.wrap(getFutute(builder));
    }
                
    //ao fazer index o objectto toIndex tem de ter um id e uma versao inseridas
    public Builder index(T toIndex) throws JsonProcessingException {
        return new Builder(this, getClient()
                .prepareIndex(getIndexName(), getTypeName())
                .setSource(ESPlugin.getPlugin().getMapper().writeValueAsBytes(toIndex)));
    }
    
    public Builder get(String id) {
        return new Builder(this, getClient()
                .prepareGet(getIndexName(), getTypeName(), id));
    }

    public Builder delete(String id) {
        return new Builder(this, getClient().prepareDelete(getIndexName(), getTypeName(), id));
    }
        
    public Builder count(Consumer<CountRequestBuilder> consumer) {
        CountRequestBuilder builder = getClient().prepareCount(getIndexName());
        if (consumer != null) consumer.accept(builder);
        return new Builder(this, builder);
    }
    
    public Builder search(Consumer<SearchRequestBuilder> consumer) {
        SearchRequestBuilder builder = getClient().prepareSearch(getIndexName()).setTypes(getTypeName());
        if (consumer != null) consumer.accept(builder);
        return new Builder(this, builder);
    }
    
    public class UpdateBuilder {

        HashMap<String, Object> fields;
        T toUpdate;
        String id;
        long version = 0;

        public UpdateBuilder(String id, long version) {
            this.fields = new HashMap<>();
            this.id = id;
            this.version = version;
        }
        
        public UpdateBuilder(String id) {
            this.fields = new HashMap<>();
            this.id = id;
        }

        public UpdateBuilder field(String field, Object object) {
            fields.put(field, object);
            return this;
        }
        
        public UpdateBuilder supply(Supplier<T> supplier) {
        	this.toUpdate = supplier.get();
        	return this;
        }

        public void updateAsync() throws JsonProcessingException {
        	UpdateRequestBuilder builder;
        	if (toUpdate == null) {
                builder = getClient().prepareUpdate(getIndexName(), getTypeName(), id)
                        .setDoc(ESPlugin.getPlugin().getMapper().writeValueAsBytes(fields))
                        .setVersion(version);
        	} else {
                builder = getClient().prepareUpdate(getIndexName(), getTypeName(), toUpdate.getId()
                			.orElseThrow(IllegalArgumentException::new))
                        .setDoc(ESPlugin.getPlugin().getMapper().writeValueAsBytes(toUpdate))
                        .setVersion(toUpdate.getVersion()
                    		.orElseThrow(IllegalArgumentException::new));
        	}
            if (version != 0) builder.setVersion(version);
            //todo add a way to log this, and to check if is really the version that has chnages. if the exception
            //was originated becauyse of other cause, we must get out of this!! --> improve testing!
            async(builder).onFailure((t) -> updateAsync());
        }

    }
    
    public String getIndexName() {
        return ESPlugin.getPlugin().indexName();
    }

    public String getTypeName() {
        return from.getAnnotation(Index.Entity.class).type();
    }
    
    public List<T> parse(SearchResponse hits) {
        List<T> beans = new ArrayList<>();
        SearchHit[] newHits = hits.getHits().getHits();
        for(int i = 0; i < newHits.length; i++) {
            final SearchHit hit = newHits[i];
            T bean = ESPlugin.getPlugin().getMapper().convertValue(hit.getSource(), from);
            bean.setId(hit.getId());
            bean.setVersion(hit.getVersion());
            beans.add(bean);
        }
        return beans;
    }

    public T parse(GetResponse result) {
        T bean = ESPlugin.getPlugin().getMapper().convertValue(result.getSource(), from);
        bean.setId(result.getId());
        bean.setVersion(result.getVersion());
        return bean;
    }
        
	@SuppressWarnings("unchecked")
	private Future<Result> getFutute(ActionRequestBuilder builder) {
    	RedeemablePromise<Result> promise = RedeemablePromise.empty();
    	builder.execute(new ActionListener<ActionResponse>() {
			@Override
			public void onFailure(Throwable t) {
				promise.failure(t);
			}
			@Override
			public void onResponse(ActionResponse response) {
				if (response instanceof CountResponse)
					promise.success(new Result<T>(((CountResponse) response).getCount(), null, null, null, null));
				if (response instanceof SearchResponse)
					promise.success(new Result<T>(null, parse(((SearchResponse) response)), null, null, null));
				if (response instanceof GetResponse)
					promise.success(new Result<T>(null, null, parse(((GetResponse) response)), null, null));
				if (response instanceof IndexResponse)
					promise.success(new Result<T>(null, null, null, (IndexResponse) response, null));
				if (response instanceof DeleteResponse)
					promise.success(new Result<T>(null, null, null, null, (DeleteResponse) response));
			}
		});
		return promise.wrapped();
    }
	
	public class Builder {

		private Finder finder;
		ActionRequestBuilder request;
		
		protected Builder(Finder finder, ActionRequestBuilder request) {
			this.finder = finder;
			this.request = request;
		}
		
		@SuppressWarnings("unchecked")
		public Promise<Result<T>> async() {
			return finder.async(request);
		}
		
		public Result<T> get() throws Exception {
			Promise<Result<T>> result = async();
			//create a logging mecanism
			result.onFailure(t -> {
				throw new Exception(t);
			});
			//find a way to get the default wait time for ES or we may need to especify on application.conf
			return result.get(5);
		}
		
		public ActionRequestBuilder getRequest() {
			return request;
		}

	}
	
    public static Client getClient() {
        return ESPlugin.getPlugin().getClient();
    }

}
