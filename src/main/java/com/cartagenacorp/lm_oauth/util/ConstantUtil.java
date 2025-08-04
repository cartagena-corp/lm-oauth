package com.cartagenacorp.lm_oauth.util;

public class ConstantUtil {

    private ConstantUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static final String PERMISSION_DENIED = "No tiene permisos para realizar esta acción";
    public static final String DATA_INTEGRITY_FAIL_MESSAGE = "Problemas con la integridad de los datos";
    public static final String RESOURCE_NOT_FOUND = "Recurso no encontrado";
    public static final String INVALID_INPUT = "Entrada inválida";
    public static final String INVALID_UUID = "El ID proporcionado no es un UUID válido";
    public static final String DUPLICATE_EMAIL = "El correo electrónico ya está en uso";
    public static final String ERROR_PROCESSING_FILE = "Error procesando el archivo";
    public static final String INTERNAL_SERVER_ERROR = "Error interno del servidor";

    public class Success {

        public static final String USER_CREATED = "Usuario creado correctamente";
        public static final String USERS_IMPORT = "Usuarios importados correctamente";
        public static final String ROLE_ASSIGNED = "Rol del usuario actualizado";

        public Success() {
            throw new IllegalStateException("Util class");
        }

    }

}
