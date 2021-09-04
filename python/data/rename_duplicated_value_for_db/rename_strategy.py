#!/usr/bin/env python3

"""
Create by tuke on 2020/10/20
"""
import abc
import uuid


class RenameStrategy(metaclass=abc.ABCMeta):
    @abc.abstractmethod
    def get_new_name(self, old_name, primary_key, timestamp=None):
        pass

    @abc.abstractmethod
    def sorted(self, tuple_list):
        pass


class UUIDRenameStrategy(RenameStrategy):
    def __init__(self, width=6, skip_first=True):
        self.width = width
        self.skip_first = skip_first
        self.name_dict = dict()

    def get_new_name(self, old_name, primary_key, timestamp=None):
        status = self.name_dict.get(old_name)
        if self.skip_first:
            # don't change the first row
            if not status:
                self.name_dict[old_name] = True
                return old_name
        # maybe conflict
        suffix = str(uuid.uuid4())[32 - self.width:32]
        return old_name + '_' + suffix

    def sorted(self, tuple_list):
        return sorted(tuple_list, key=lambda it: it[0], reverse=True)

    @staticmethod
    def new():
        return UUIDRenameStrategy()


class SeqRenameStrategy(RenameStrategy):
    def __init__(self, skip_first=True):
        self.skip_first = skip_first
        self.name_dict = dict()

    def get_new_name(self, old_name, primary_key, timestamp=None):
        status = self.name_dict.get(old_name)
        if status is None:
            status = 0

        if self.skip_first:
            # don't change the first row
            if status == 0:
                self.name_dict[old_name] = 1
                return old_name
        # maybe conflict
        self.name_dict[old_name] = status + 1
        return old_name + '_' + str(status)

    def sorted(self, tuple_list):
        return sorted(tuple_list, key=lambda it: it[0], reverse=True)

    @staticmethod
    def new():
        return SeqRenameStrategy()


STRATEGY_CONSTRUCTOR_DICT = {
    'seq': SeqRenameStrategy.new,
    'uuid': UUIDRenameStrategy.new,
}
