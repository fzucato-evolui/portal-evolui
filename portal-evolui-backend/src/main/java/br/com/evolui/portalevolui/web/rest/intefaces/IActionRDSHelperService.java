package br.com.evolui.portalevolui.web.rest.intefaces;

import br.com.evolui.portalevolui.web.beans.ActionRDSBean;
import br.com.evolui.portalevolui.web.rest.dto.aws.BackupRestoreRDSDTO;
import br.com.evolui.portalevolui.web.rest.dto.aws.RDSDTO;

import java.sql.SQLException;
import java.util.LinkedHashSet;

public interface IActionRDSHelperService {
    LinkedHashSet<String> retrieveRDSSchemas(RDSDTO dto) throws SQLException;
    LinkedHashSet<String> retrieveRDSTablespaces(RDSDTO dto) throws SQLException;
    void backup(ActionRDSBean bean);
    void restore(ActionRDSBean bean);
    void clone(ActionRDSBean bean);
    BackupRestoreRDSDTO getBackupRestore(Long id);

}
