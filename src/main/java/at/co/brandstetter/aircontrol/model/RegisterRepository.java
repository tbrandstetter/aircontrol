package at.co.brandstetter.aircontrol.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegisterRepository extends CrudRepository <RegisterEntity, Integer> {

}
