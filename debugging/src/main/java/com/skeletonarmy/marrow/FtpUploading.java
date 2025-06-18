package com.skeletonarmy.marrow;

import static com.skeletonarmy.marrow.GetConfigValue.getValue;

import com.skeletonarmy.marrow.MarrowExceptions.ConfigurationException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilters;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for uploading files to an FTP server.
 * <p>
 * Uses Apache Commons Net FTPClient to handle FTP communication.
 */
public class FtpUploading {
    /**
     * ASCII file transfer mode constant.
     */
    public static final int ASCII = 0;

    /**
     * Binary file transfer mode constant.
     */
    public static final int BINARY = 2;

    /**
     * FTP client used for communication with the FTP server.
     */
    private FTPClient ftp;
    public String serverIp;
    public int port;
    public String userName;
    public String password;

    /**
     * Constructs a new {@code FtpUploading} instance and attempts to connect to the FTP server
     * using credentials and connection details loaded from a configuration file located at
     * {@code /sdcard/config/Marrow.conf}.
     * <p>
     * The configuration file must contain the following keys:
     * <ul>
     *   <li>{@code FtpIp} – the FTP server's IP address</li>
     *   <li>{@code FtpPort} – the FTP server's port number</li>
     *   <li>{@code FtpUsername} – the username for authentication</li>
     *   <li>{@code FtpPassword} – the password for authentication</li>
     * </ul>
     * If all required values are present and valid, this constructor will automatically attempt
     * to establish a connection to the FTP server. If the configuration is missing, malformed, or
     * invalid, an exception will be thrown.
     * <p>
     * Configuration can be created manually or generated using the MarrowConfig tool.
     *
     * @throws IOException if an error occurs while reading the configuration file or when connecting to the FTP server
     * @throws ConfigurationException if the configuration is missing required keys or fails validation
     **/
    public FtpUploading() throws IOException, ConfigurationException {
        this.serverIp = getValue("FtpIp");
        this.port = Integer.parseInt(Objects.requireNonNull(getValue("FtpPort")));
        this.userName = getValue("FtpUsername");
        this.password = getValue("FtpPassword");
        if (isConfigValid()) {
            Connect();
        } else {
            throw new ConfigurationException("An error occurred in the configuration, Please regenerate it using MarrowConfig (add link later)");
        }
    }

    /**
     * Checks if the client is connected
     *
     * @return If client is connected to the server
     */
    public boolean IsConnected() {
        return this.ftp.isConnected();
    }

    /**
     * Establishes a connection to the FTP server using configuration from {@link GetConfigValue}.
     *
     * @throws IOException if the connection or login fails.
     */
    private void Connect() throws IOException {
        this.ftp = new FTPClient();
        this.ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));

        this.ftp.connect(this.serverIp, this.port);
        int reply = this.ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            this.ftp.disconnect();
            throw new IOException("Exception in connecting to FTP Server");
        }

        this.ftp.login(this.userName, this.password);
    }

    /**
     * Disconnects from the FTP server.
     *
     * @throws IOException if an I/O error occurs while disconnecting.
     */
    public void exit() throws IOException {
        this.ftp.disconnect();
    }

    /**
     * Retrieves the last reply code from the FTP server.
     *
     * @return the reply code.
     */
    public int GetReplyCode() {
        return this.ftp.getReplyCode();
    }

    /**
     * Retrieves the last reply string from the FTP server.
     *
     * @return the reply string.
     */
    public String GetReplyString() {
        return this.ftp.getReplyString();
    }

    /**
     * Uploads a file to the FTP server.
     *
     * @param LocalFilePath the local file path to upload.
     * @param RemotePath    the remote destination path on the FTP server.
     * @param FileType      the file transfer mode (use {@link #ASCII} or {@link #BINARY}).
     * @param DeleteAfter   delete the local file after upload
     * @throws IOException if the file doesn't exist or upload fails.
     */
    public void UploadFile(String LocalFilePath, String RemotePath, int FileType, boolean DeleteAfter) throws Exception {
        File FileToUpload = new File(LocalFilePath);
        if (FileType == 0 || FileType == 2) {
            this.ftp.setFileType(FileType);
            if (FileToUpload.exists()) {
                this.ftp.storeFile(RemotePath, new FileInputStream(FileToUpload));
                if (DeleteAfter) {
                    FileToUpload.delete();
                }
            } else {
                throw new IOException("Input file does not exist");
            }
        } else {
            throw new Exception("Filetype isnt set to ASCII or Binary");
        }
    }

    /**
     * Uploads a file to a remote FTP server.
     *
     * <p>This method sets the file type for the FTP client and uploads a local file to the specified remote path.
     * If the local file does not exist, it throws an {@link IOException}.</p>
     *
     * @param LocalFilePath the {@link File} object representing the path to the local file to be uploaded
     * @param RemotePath    the destination path on the remote FTP server where the file should be uploaded
     * @param FileType      the file type for FTP transmission
     * @param DeleteSrc   delete the local file after upload
     * @throws IOException if the local file does not exist or if an I/O error occurs during upload
     */
    public void UploadFile(File LocalFilePath, String RemotePath, int FileType, boolean DeleteSrc) throws Exception {
        if (FileType == 0 || FileType == 2) {
            this.ftp.setFileType(FileType);
            if (LocalFilePath.exists()) {
                this.ftp.storeFile(RemotePath, new FileInputStream(LocalFilePath));
                if (DeleteSrc) {
                    LocalFilePath.delete();
                }
            } else {
                throw new IOException("Input file does not exist");
            }
        } else {
            throw new Exception("Filetype isnt set to ASCII or Binary");
        }

    }

    /**
     * Retrieves a list of file names from the current directory of the connected FTP server.
     *
     * @return A list of file names (as Strings) in the current FTP directory.
     * @throws IOException If an I/O error occurs while communicating with the FTP server.
     */
    public List<String> ListFiles() throws IOException {
        FTPFile[] list = this.ftp.listFiles();
        List<String> FileList = new ArrayList<>();
        for (FTPFile file : list) {
            FileList.add(file.getName());
        }
        return FileList;
    }

    /**
     * Retrieves a list of filenames from the specified path on the FTP server.
     *
     * @param path The remote directory path from which to list files.
     * @return A list of file names (as Strings) from the given path.
     * @throws IOException If an I/O error occurs while accessing the FTP server.
     */
    public List<String> ListFiles(String path) throws IOException {
        FTPFile[] list = this.ftp.listFiles(path);
        List<String> FileList = new ArrayList<>();
        for (FTPFile file : list) {
            FileList.add(file.getName());
        }
        return FileList;
    }

    /**
     * Retrieves a list of {@link FTPFile} objects from the current working directory on the FTP server.
     *
     * @return An array of {@link FTPFile} representing files and directories in the current directory.
     * @throws IOException If an I/O error occurs while accessing the FTP server.
     */
    public FTPFile[] getFTPFiles() throws IOException {
        return this.ftp.listFiles();
    }

    /**
     * Retrieves a list of {@link FTPFile} objects from the specified directory on the FTP server.
     *
     * @param path The remote directory path to list files from.
     * @return An array of {@link FTPFile} representing files and directories in the specified path.
     * @throws IOException If an I/O error occurs while accessing the FTP server.
     */
    public FTPFile[] getFTPFiles(String path) throws IOException {
        return this.ftp.listFiles(path);
    }

    /** Retrieves an array of FTPFile objects representing the directories located at the specified path on the FTP server.
     *
     * @param path the path on the FTP server to search for directories
     * @return an array of FTPFile objects that are directories within the specified path
     * @throws IOException if an I/O error occurs while communicating with the FTP server
     */
    public FTPFile[] getFTPDirectories(String path) throws IOException {
        return this.ftp.listFiles(path, FTPFileFilters.DIRECTORIES);
    }

    /**
     * Retrieves an array of FTPFile objects representing the directories located in the current working directory
     * of the FTP session.
     *
     * @return an array of FTPFile objects that are directories in the current working directory
     * @throws IOException if an I/O error occurs while communicating with the FTP server
     */
    public FTPFile[] getFTPDirectories() throws IOException {
        return this.ftp.listDirectories();
    }

    /**
     * Downloads a file from a remote FTP server to a local destination file path.
     *
     * @param RemoteSrcFile   Path to the remote source file on the FTP server.
     * @param LocalDestFile   Local file path where the file should be saved.
     * @param FileType        Type of file transfer: 0 for ASCII, 2 for BINARY.
     * @param DeleteRemote    If true, deletes the file from the remote FTP server after download.
     * @throws IOException    If destination file exists, file type is invalid, or an I/O error occurs during download.
     */
    public void DownloadFile(String RemoteSrcFile, String LocalDestFile, int FileType, boolean DeleteRemote) throws IOException {
        File Dest = new File(LocalDestFile);
        FileOutputStream DestStream = new FileOutputStream(Dest);
        if (FileType == 0 || FileType == 2) {
            this.ftp.setFileType(FileType);
            if (Dest.exists()) {
                this.ftp.retrieveFile(RemoteSrcFile, DestStream);
                if (DeleteRemote) {
                    this.ftp.deleteFile(RemoteSrcFile);
                }
            } else {
                throw new IOException("Destination file already exists");
            }
        } else {
            throw new IOException("FileType isnt set to ASCII or BINARY");
        }
    }

    /**
     * Downloads a file from a remote FTP server to a specified local File object.
     *
     * @param RemoteSrcFile   Path to the remote source file on the FTP server.
     * @param LocalDestFile   File object pointing to where the file should be saved.
     * @param FileType        Type of file transfer: 0 for ASCII, 2 for BINARY.
     * @param DeleteRemote    If true, deletes the file from the remote FTP server after download.
     * @return                The downloaded local File object.
     * @throws IOException    If destination file exists, file type is invalid, or an I/O error occurs during download.
     */
    public File DownloadFile(String RemoteSrcFile, File LocalDestFile, int FileType, boolean DeleteRemote) throws IOException {
        FileOutputStream DestStream = new FileOutputStream(LocalDestFile);
        if (FileType == 0 || FileType == 2) {
            this.ftp.setFileType(FileType);
            if (LocalDestFile.exists()) {
                this.ftp.retrieveFile(RemoteSrcFile, DestStream);
                if (DeleteRemote) {
                    this.ftp.deleteFile(RemoteSrcFile);
                }
                return LocalDestFile;
            } else {
                throw new IOException("Destination file already exists");
            }
        } else {
            throw new IOException("FileType isnt set to ASCII or BINARY");
        }
    }

    /**
     * Checks whether the FTP configuration is valid by ensuring that
     * default placeholder values are not being used.
     *
     * @return true if all FTP configuration fields (Server, User, Password)
     *         have been set to values other than the default placeholders;
     *         false otherwise.
     */
    private boolean isConfigValid() {
       return  this.serverIp != null &&
               this.userName != null &&
               this.password != null;
    }

    /**
     * Logs out from the FTP server.
     *
     * @throws IOException if an I/O error occurs during logout
     */
    public void logout() throws IOException {
        this.ftp.logout();
    }

    /**
     * Logs out from the FTP server and then disconnects the FTP session.
     * <p>
     * This method calls {@code ftp.logout()} followed by {@code ftp.disconnect()} to ensure
     * the session is cleanly terminated and the connection is properly closed.
     * </p>
     *
     * @throws IOException if an I/O error occurs during logout or disconnect
     */
    public void disconnect() throws IOException {
        this.ftp.logout();
        this.ftp.disconnect();
    }

    /**
     * Changes the current working directory on the FTP server to the specified path.
     *
     * @param path the absulute path to which the working directory should be changed.
     *
     * @return {@code true} if the working directory was successfully changed, {@code false} otherwise.
     * @throws IOException if an I/O error occurs while communicating with the FTP server.
     */
    public boolean changeWorkingDirectory(String path) throws IOException {
        return this.ftp.changeWorkingDirectory(path);
    }

    /**
     * Gets working directory.
     *
     * @return the working directory
     * @throws IOException if an I/O error occurs while communicating with the FTP server.
     */
    public String getWorkingDirectory() throws IOException {
        return this.ftp.printWorkingDirectory();
    }

    /**
     * Creates a directory at the specified path on the FTP server.
     *
     * @param path the directory path to create on the FTP server
     * @return {@code true} if the directory was created successfully; {@code false} otherwise
     * @throws IOException if an I/O error occurs during directory creation
     */
    public boolean mkdir(String path) throws IOException {
       return this.ftp.makeDirectory(path);
    }

    /**
     * Compresses the specified source file into a GZIP file, uploads it to the FTP server,
     * and optionally deletes the source file and/or the GZIP file after upload.
     *
     * @param srcFile       the file to compress and upload
     * @param dstPath       the destination path for the GZIP file; if {@code null}, defaults to the same directory as {@code srcFile}
     * @param gzipName      the desired name for the compressed file
     * @param deleteSrcFile if {@code true}, deletes the original source file after compression
     * @param deleteGzip    if {@code true}, deletes the compressed file after uploading
     * @throws IOException if an I/O error occurs during compression, deletion, renaming, or upload
     */
    public void conpressAndUploadGzip(@NotNull File srcFile, String dstPath, String gzipName,boolean deleteSrcFile, boolean deleteGzip) throws IOException {
        if (dstPath == null) {
           dstPath = srcFile.getParentFile().getAbsolutePath() + "/" + srcFile.getName() + ".gz";
        } else {
            dstPath = dstPath + "/" + srcFile.getName() + ".gz";
        }
        try {
            byte[] buffer = new byte[1024];
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(new FileOutputStream(dstPath));
            FileInputStream fileInputStream = new FileInputStream(srcFile);
            int totalSize;
            while ((totalSize = fileInputStream.read(buffer)) > 0) {
                gzipOutputStream.write(buffer, 0, totalSize);
            }
            if (deleteSrcFile) {
                if (!srcFile.delete()) {
                    throw new IOException("File deletion failed");
                }
            }
            File gzipFile = new File(dstPath);
            if (!gzipName.endsWith(".gz")) {
                gzipName = gzipName + ".gz";
            }
            if (!gzipFile.renameTo(new File(gzipFile, gzipName))) {
                throw new IOException("Gzip file rename failed");
            }
            UploadFile(gzipFile, "/" + gzipFile.getName(), FtpUploading.BINARY, deleteGzip);
        } catch (IOException e) {
            throw new IOException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Compresses the specified source file path into a GZIP file, uploads it to the FTP server,
     * and optionally deletes the source file and/or the GZIP file after upload.
     *
     * @param srcFile       the path of the file to compress and upload
     * @param dstPath       the destination path for the GZIP file; if {@code null}, defaults to the same directory as {@code srcFile}
     * @param gzipName      the desired name for the compressed file
     * @param deleteSrcFile if {@code true}, deletes the original source file after compression
     * @param deleteGzip    if {@code true}, deletes the compressed file after uploading
     * @throws IOException if an I/O error occurs during compression, deletion, renaming, or upload
     */
    public void conpressAndUploadGzip(@NotNull String srcFile, String dstPath, String gzipName, boolean deleteSrcFile, boolean deleteGzip) throws IOException {
        File localFile = new File(srcFile);
        if (dstPath == null) {
            dstPath = localFile.getParentFile().getAbsolutePath() + "/" + localFile.getName() + ".gz";
        } else {
            dstPath = dstPath + "/" + localFile.getName() + ".gz";
        }
        try {
            byte[] buffer = new byte[1024];
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(new FileOutputStream(dstPath));
            FileInputStream fileInputStream = new FileInputStream(localFile);
            int totalSize;
            while ((totalSize = fileInputStream.read(buffer)) > 0) {
                gzipOutputStream.write(buffer, 0, totalSize);
            }
            if (deleteSrcFile) {
                if (!localFile.delete()) {
                    throw new IOException("File deletion failed");
                }
            }
            File gzipFile = new File(dstPath);
            if (!gzipName.endsWith(".gz")) {
                gzipName = gzipName + ".gz";
            }
            if (!gzipFile.renameTo(new File(gzipFile, gzipName))) {
                throw new IOException("Gzip file rename failed");
            }
            UploadFile(gzipFile, "/" + gzipFile.getName(), FtpUploading.BINARY, deleteGzip);
        } catch (IOException e) {
            throw new IOException(e);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    /**
     * Compresses the specified source file into a ZIP archive and uploads it.
     *
     * @param srcFile        The source file to be compressed.
     * @param dstPath        The destination directory where the ZIP file should be created.
     *                       If null, the ZIP will be placed in the same directory as {@code srcFile}.
     * @param zipName        The name to assign to the ZIP file after compression.
     *                       The method ensures the name ends with ".zip".
     * @param deleteSrcFile  If true, deletes the original source file after compression.
     * @param deleteZip      If true, deletes the ZIP file after it has been uploaded.
     * @throws IOException   If any IO-related operation (e.g., reading, writing, deleting, renaming) fails.
     */
    public void compressAndUploadZip(@NotNull File srcFile, String dstPath, String zipName, boolean deleteSrcFile, boolean deleteZip) throws IOException {
        if (dstPath == null) {
            dstPath = srcFile.getParentFile().getAbsolutePath() + "/" + srcFile.getName() + ".zip";
        } else {
            dstPath = dstPath + "/" + srcFile.getName() + ".zip";
        }
        try {
            byte[] buffer = new byte[1024];
            ZipOutputStream zipOutputStream = new ZipOutputStream( new FileOutputStream(dstPath));
            FileInputStream fileInputStream = new FileInputStream(srcFile);
            int totalSize;
            while ((totalSize = fileInputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, totalSize);
            }
            if (deleteSrcFile) {
                if (!srcFile.delete()) {
                    throw new IOException("File deletion failed");
                }
            }
            File zipFile = new File(dstPath);
            if (!zipName.endsWith(".gz")) {
                zipName = zipName + ".zip";
            }
            if (!zipFile.renameTo(new File(zipFile, zipName))) {
                throw new IOException("Zip file rename failed");
            }
            UploadFile(zipFile, "/" + zipFile.getName(), FtpUploading.BINARY, deleteZip);
        } catch (IOException e) {
            throw new IOException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Compresses the specified source file path into a ZIP archive and uploads it.
     *
     * @param srcFile        The absolute path to the source file to be compressed.
     * @param dstPath        The destination directory where the ZIP file should be created.
     *                       If null, the ZIP will be placed in the same directory as {@code srcFile}.
     * @param zipName        The name to assign to the ZIP file after compression.
     *                       The method ensures the name ends with ".zip".
     * @param deleteSrcFile  If true, deletes the original source file after compression.
     * @param deleteZip      If true, deletes the ZIP file after it has been uploaded.
     * @throws IOException   If any IO-related operation (e.g., reading, writing, deleting, renaming) fails.
     */
    public void compressAndUploadZip (@NotNull String srcFile, String dstPath, String zipName, boolean deleteSrcFile, boolean deleteZip) throws IOException {
        File localFile = new File(srcFile);
        if (dstPath == null) {
            dstPath = localFile.getParentFile().getAbsolutePath() + "/" + localFile.getName() + ".zip";
        } else {
            dstPath = dstPath + "/" + localFile.getName() + ".zip";
        }
        try {
            byte[] buffer = new byte[1024];
            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(dstPath));
            FileInputStream fileInputStream = new FileInputStream(localFile);
            int totalSize;
            while ((totalSize = fileInputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, totalSize);
            }
            if (deleteSrcFile) {
                if (!localFile.delete()) {
                    throw new IOException("File deletion failed");
                }
            }
            File ZipFile = new File(dstPath);
            if (!zipName.endsWith(".zip")) {
                zipName = zipName + ".zip";
            }
            if (!ZipFile.renameTo(new File(ZipFile, zipName))) {
                throw new IOException("Zip file rename failed");
            }
            UploadFile(ZipFile, "/" + ZipFile.getName(), FtpUploading.BINARY, deleteZip);
        } catch (IOException e) {
            throw new IOException(e);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    /**
     * Creates a TAR archive from the specified list of file paths.
     *
     * @param filePaths  An array of file paths to include in the TAR archive.
     * @param dstTar     The desired destination path and filename for the resulting TAR archive.
     *                   If it does not end with ".tar", the method appends the extension.
     * @return A {@link File} object representing the created TAR archive.
     * @throws IOException If any file cannot be read, doesn't exist, or an IO operation fails during archiving.
     */
    public static File createTarFile (@NotNull String[] filePaths, String dstTar) throws IOException {
        if (!dstTar.endsWith(".tar")) {
           dstTar = dstTar + ".tar";
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(dstTar);
            TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(fileOutputStream);
            for (String path : filePaths) {
                File file = new File(path);
                if (!file.exists()) {
                    throw new IOException("Input file " + path + " was not found");
                }

                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(file, file.getName());
                    tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);
                    IOUtils.copy(fileInputStream, tarArchiveOutputStream);
                    tarArchiveOutputStream.closeArchiveEntry();
                }
            }
            tarArchiveOutputStream.finish();
        } catch (IOException e) {
            throw new IOException(e);
        }
        return new File(dstTar);
    }
}