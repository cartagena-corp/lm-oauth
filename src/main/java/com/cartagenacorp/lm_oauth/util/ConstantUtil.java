package com.cartagenacorp.lm_oauth.util;

public class ConstantUtil {

    private ConstantUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static final String PERMISSION_DENIED = "No tiene permisos para realizar esta acción";
    public static final String TOKEN_ERROR = "No se pudo obtener el token de seguridad";
    public static final String DATA_INTEGRITY_FAIL_MESSAGE = "Problemas con la integridad de los datos";
    public static final String RESOURCE_NOT_FOUND = "Recurso no encontrado";
    public static final String INVALID_INPUT = "Entrada inválida";
    public static final String INVALID_UUID = "El ID proporcionado no es un UUID válido";
    public static final String DUPLICATE_EMAIL = "El correo electrónico ya está en uso";
    public static final String ROLE_NOT_FOUND = "El rol no existe";
    public static final String ORGANIZATION_NOT_FOUND = "La organización no existe";
    public static final String ERROR_PROCESSING_FILE = "Error procesando el archivo";
    public static final String INTERNAL_SERVER_ERROR = "Error interno del servidor";
    public static final String ACCESS_EXCEPTION = "El servicio externo no está disponible o no se pudo acceder a él";
    public static final String EXCLUSIVE_SUPER_ADMIN_ROL = "El rol SUPER_ADMIN es reservado para el administrador de LA MURALLA";

    public class Success {

        public static final String USER_CREATED = "Usuario creado correctamente";
        public static final String USER_DELETED = "Usuario eliminado correctamente";
        public static final String USERS_IMPORT = "Usuarios importados correctamente";
        public static final String ROLE_ASSIGNED = "Rol del usuario actualizado";
        public static final String USERS_OBTAINED = "Listado de usuarios obtenido";

        public Success() {
            throw new IllegalStateException("Util class");
        }

    }

}
