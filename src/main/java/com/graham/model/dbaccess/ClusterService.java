package com.graham.model.dbaccess;

import java.util.List;
import java.util.UUID;

import org.mortbay.log.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.graham.model.Cluster;

@Repository
public class ClusterService {

	@Autowired
	private MongoTemplate mongoTemplate;

	public static final String COLLECTION_NAME = "clusters";

	public Cluster addCluster(Cluster cluster) {
		if (!mongoTemplate.collectionExists(Cluster.class)) {
			mongoTemplate.createCollection(Cluster.class);
		}		
		cluster.setId(UUID.randomUUID().toString());
		mongoTemplate.insert(cluster, COLLECTION_NAME);
		Log.info("SAVE SUCCESS!");
		return cluster;
	}

	public Cluster getCluster(String id) {
		return mongoTemplate.findById(id, Cluster.class, COLLECTION_NAME);
	}

	public List<Cluster> listClusters() {
		return mongoTemplate.findAll(Cluster.class, COLLECTION_NAME);
	}

	public List<Cluster> listClusterById(String id) {
		Query q = new Query();
		q.addCriteria(Criteria.where("id").is(id));
		return mongoTemplate.find(q, Cluster.class, COLLECTION_NAME);
	}

	public void deleteCluster(Cluster cluster) {
		mongoTemplate.remove(cluster, COLLECTION_NAME);
	}

	public void updateCluster(Cluster cluster) {
		Cluster clusterToUpdate = mongoTemplate.findOne(Query.query(Criteria.where("id").is(cluster.getId())), Cluster.class);
		clusterToUpdate = cluster;
		mongoTemplate.save(clusterToUpdate, COLLECTION_NAME);
	}
}
