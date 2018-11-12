package tigase.shiku.db;

import tigase.db.Repository;
import tigase.shiku.model.MessageModel;
import tigase.shiku.model.MucMessageModel;

public interface ShikuMessageArchiveRepository extends Repository {

	void archiveMessage(MessageModel model);
	//保存公众号相关
	void archivePubMessage(MessageModel model);

	void archiveMessage(MucMessageModel model);
	
//	List<Integer> userIdList(String roomId);
}
