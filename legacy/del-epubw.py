import os
import os.path


def walk_a_dir(pathname_str, func):
    for foldername, subfolders, filenames in os.walk(pathname_str):
        print('The current folder is ' + foldername)
        for subfolder in subfolders:
            print('The current folder is ' + subfolder)
            func(foldername, subfolder)

    for foldername, subfolders, filenames in os.walk(pathname_str):
        print('The current folder is ' + foldername)
        for filename in filenames:
            print('The current file is ' + filename)
            func(foldername, filename)


epubw = 'ePUBw.COM - '


def rename_epubw(foldername, filename):
    if epubw in filename:
        new_filename = filename[len(epubw):]
        print('rename %s to %s' % (filename, new_filename))
        os.rename(os.path.join(foldername, filename),
                  os.path.join(foldername, new_filename))


walk_a_dir('C:\\Users\\tuke\\3D Objects\\', rename_epubw)
