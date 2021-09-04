import os
import os.path
import shutil


def copy_ebook(source, kind, target_root_dir):
    target = os.path.join(target_root_dir, kind)
    if not os.path.exists(target):
        os.mkdir(target)
    target = os.path.join(target, os.path.basename(source))
    print('copying %s to %s' % (source, target))
    shutil.copyfile(source, target)


def copy_ebook_only(source, kind, target_root_dir):
    target = os.path.join(target_root_dir, kind)
    if not os.path.exists(target):
        os.mkdir(target)
    target = os.path.join(target, os.path.basename(source))
    print('copying %s to %s' % (source, target))
    shutil.copyfile(source, target)


def move_ebooks(pathname_from: str, pathname_to: str, pathname_err: str):
    if not os.path.exists(pathname_to):
        os.mkdir(pathname_to)
    if not os.path.exists(pathname_err):
        os.mkdir(pathname_err)
    print('start to move on')
    kinds = os.listdir(pathname_from)
    for kind in kinds:
        kind_dir = os.path.join(pathname_from, kind)
        if not os.path.isdir(kind_dir):
            continue
        books = os.listdir(kind_dir)
        for book in books:
            book_dir = os.path.join(kind_dir, book)
            if os.path.isfile(book_dir):
                # just copy it
                copy_ebook_only(book_dir, kind, pathname_to)
                continue

            ebooks = [os.path.join(book_dir, ebook)
                      for ebook in os.listdir(book_dir)]

            # first try to copy .epub
            has_epub = False
            for ebook in ebooks:
                if ebook.endswith('.epub'):
                    copy_ebook(ebook, kind, pathname_to)
                    has_epub = True
            if has_epub:
                continue

            # second try to copy .mobi
            has_mobi = False
            for ebook in ebooks:
                if ebook.endswith('.mobi'):
                    copy_ebook(ebook, kind, pathname_to)
                    has_mobi = True
            if has_mobi:
                continue

            # then try to copy .txt
            has_text = False
            for ebook in ebooks:
                if ebook.endswith('.txt'):
                    copy_ebook(ebook, kind, pathname_to)
                    has_text = True
            if has_text:
                continue

            # finally copy tree
            print(' no epub, mobi or text in %s' % book_dir)
            book_dir_target = os.path.join(pathname_err, kind)
            book_dir_target = os.path.join(book_dir_target, book)
            shutil.copytree(book_dir, book_dir_target)
    print('end')


pathname_from = 'C:\\Users\\tuke\\Videos'
pathname_to = 'C:\\Users\\tuke\\Music'
pathname_err = 'C:\\Users\\tuke\\3D Objects'
move_ebooks(pathname_from, pathname_to, pathname_err)
