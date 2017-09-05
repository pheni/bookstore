package cu.pdi.bookstore.domain.inventory.title;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * Created by
 * taiyou on 8/27/17.
 */
@Embeddable
@NoArgsConstructor
@RequiredArgsConstructor
@Getter
public class EditionYear implements Serializable {
    @NonNull
    Integer editionYear;
}
